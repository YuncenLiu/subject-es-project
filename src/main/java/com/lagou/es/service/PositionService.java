package com.lagou.es.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PositionService {
    /* 分页查询 */
    List<Map<String,Object>>  searchPos(String keyword,int pageNo,int pageSize)throws  IOException;
    List<Map<String, Object>> searchSubject(String keyword, int pageNo, int pageSize)throws IOException;

    /**导入数据 */
    void   importAll()throws IOException;
}
