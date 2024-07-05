package com.lagou.es.service.impl;

import com.lagou.es.service.PositionService;
import com.lagou.es.util.DBHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;


@Service
public class PositionServiceImpl  implements PositionService {
    private  static  final Logger  logger = LogManager.getLogger(PositionServiceImpl.class);
    @Autowired
    private RestHighLevelClient  client;
    private  static  final  String  POSITION_INDEX = "position";
    private  static  final  String  SUBJECT_INDEX = "s_subject";

    @Override
    public List<Map<String, Object>> searchPos(String keyword, int pageNo, int pageSize) throws IOException {
        if (pageNo <= 1){
            pageNo = 1;
        }

        // 搜索
        SearchRequest  searchRequest = new SearchRequest(POSITION_INDEX);

        SearchSourceBuilder  searchSourceBuilder = new SearchSourceBuilder();
        // 分页设置
        searchSourceBuilder.from((pageNo-1)*pageSize);
        searchSourceBuilder.size(pageSize);
        QueryBuilder builder = QueryBuilders.matchQuery("positionName",keyword);
        searchSourceBuilder.query(builder);
        searchSourceBuilder.timeout(new TimeValue(60,TimeUnit.SECONDS));
        // 执行搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse  searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
        ArrayList<Map<String,Object>>  list = new ArrayList<>();
        SearchHit[]  hits = searchResponse.getHits().getHits();
        for (SearchHit hit:hits){
            list.add(hit.getSourceAsMap());
        }

        return   list;
    }
    @Override
    public List<Map<String, Object>> searchSubject(String keyword, int pageNo, int pageSize) throws IOException{
        if (pageNo <= 1){
            pageNo = 1;
        }

        // 搜索
        SearchRequest  searchRequest = new SearchRequest(SUBJECT_INDEX);

        SearchSourceBuilder  searchSourceBuilder = new SearchSourceBuilder();
        // 分页设置
        searchSourceBuilder.from((pageNo-1)*pageSize);
        searchSourceBuilder.size(pageSize);
        QueryBuilder builder = QueryBuilders.matchQuery("sub_title",keyword);
        searchSourceBuilder.query(builder);
        searchSourceBuilder.timeout(new TimeValue(60,TimeUnit.SECONDS));
        // 执行搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse  searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
        ArrayList<Map<String,Object>>  list = new ArrayList<>();
        SearchHit[]  hits = searchResponse.getHits().getHits();
        for (SearchHit hit:hits){
            list.add(hit.getSourceAsMap());
        }

        return   list;
    }

    @Override
    public void importAll() throws IOException {
         writeMySQLDataToES("position");
    }



    private   void  writeMySQLDataToES(String tableName){
        BulkProcessor  bulkProcessor  = getBulkProcessor(client);
        Connection  connection = null;
        PreparedStatement  ps = null;
        ResultSet  rs = null;
        try {
            connection = DBHelper.getConn();
            logger.info("start handle data :" + tableName);
            String  sql = "select * from " + tableName;
            ps = connection.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            // 根据自己需要设置 fetchSize
            ps.setFetchSize(20);
            rs = ps.executeQuery();
            ResultSetMetaData  colData = rs.getMetaData();
            ArrayList<HashMap<String,String>> dataList = new ArrayList<>();
            HashMap<String,String>  map = null;
            int  count = 0;
            // c 就是列的名字   v 就是列对应的值
            String  c = null;
            String  v = null;
            while(rs.next()){
                count ++;
                map = new HashMap<String,String>(128);
                for (int i=1;i< colData.getColumnCount();i++){
                    c = colData.getColumnName(i);
                    v = rs.getString(c);
                    map.put(c,v);
                }
                dataList.add(map);
                // 每1万条 写一次   不足的批次的数据 最后一次提交处理
                if (count % 10000 == 0){
                    logger.info("mysql handle data  number:"+count);
                    // 将数据添加到 bulkProcessor
                    for (HashMap<String,String> hashMap2 : dataList){
                        bulkProcessor.add(new IndexRequest(POSITION_INDEX).source(hashMap2));
                    }
                    // 每提交一次 清空 map 和  dataList
                    map.clear();
                    dataList.clear();
                }
            }
            // 处理 未提交的数据
            for (HashMap<String,String> hashMap2 : dataList){
                bulkProcessor.add(new IndexRequest(POSITION_INDEX).source(hashMap2));
            }
            bulkProcessor.flush();

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                rs.close();
                ps.close();
                connection.close();
                boolean  terinaFlag = bulkProcessor.awaitClose(150L,TimeUnit.SECONDS);
                logger.info(terinaFlag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    /**
     * 获取BulkProcessor实例，用于批量处理Elasticsearch的索引操作。
     * 这样可以优化性能，通过聚合多个操作一次性提交到Elasticsearch。
     *
     * @param client 高级REST客户端，用于执行bulk请求。
     * @return BulkProcessor实例，用于批量处理索引操作。
     */
    private BulkProcessor getBulkProcessor(RestHighLevelClient client) {
        BulkProcessor bulkProcessor = null;
        try {
            // 创建BulkProcessor监听器，用于在批量处理前后执行自定义操作。
            BulkProcessor.Listener listener = new BulkProcessor.Listener() {
                @Override
                public void beforeBulk(long executionId, BulkRequest request) {
                    // 在执行批量请求前记录日志，显示即将插入的数据数量。
                    logger.info("Try to insert data number : "
                            + request.numberOfActions());
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request,
                                      BulkResponse response) {
                    // 在批量请求成功后记录日志，显示成功插入的数据数量和执行ID。
                    logger.info("************** Success insert data number : "
                            + request.numberOfActions() + " , id: " + executionId);
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                    // 在批量请求失败后记录日志，显示失败信息和执行ID。
                    logger.error("Bulk is unsuccess : " + failure + ", executionId: " + executionId);
                }
            };

            // 定义bulk操作的消费者，将请求提交给客户端的bulkAsync方法。
            BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer = (request, bulkListener) -> client
                    .bulkAsync(request, RequestOptions.DEFAULT, bulkListener);

            // 使用builder模式配置并构建BulkProcessor。
            BulkProcessor.Builder builder = BulkProcessor.builder(bulkConsumer, listener);
            builder.setBulkActions(5000); // 设置触发批量提交的操作数阈值。
            builder.setBulkSize(new ByteSizeValue(100L, ByteSizeUnit.MB)); // 设置触发批量提交的大小阈值。
            builder.setConcurrentRequests(10); // 设置允许并发的bulk请求数量。
            builder.setFlushInterval(TimeValue.timeValueSeconds(100L)); // 设置自动触发批量提交的间隔。
            builder.setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueSeconds(1L), 3)); // 设置重试策略。

            // 构建BulkProcessor实例。
            // 注意点：让参数设置生效
            bulkProcessor = builder.build();

        } catch (Exception e) {
            // 处理构建BulkProcessor过程中可能出现的异常。
            e.printStackTrace();
            try {
                // 如果bulkProcessor已初始化，尝试关闭它以释放资源。
                bulkProcessor.awaitClose(100L, TimeUnit.SECONDS);
            } catch (Exception e1) {
                // 记录关闭bulkProcessor时出现的异常。
                logger.error(e1.getMessage());
            }
        }
        return bulkProcessor;
    }

}

