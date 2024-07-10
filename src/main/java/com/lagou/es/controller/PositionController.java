package com.lagou.es.controller;

import com.lagou.es.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class PositionController {
    @Autowired
    private PositionService  service;

    // 测试页面
    @GetMapping({"/","/index"})
    public   String  indexPage(){
        return  "index";
    }

    @GetMapping({"/subject"})
    public   String  subjectPage(){
        return  "subject";
    }


    @GetMapping("/search/{keyword}/{pageNo}/{pageSize}")
    @ResponseBody
    public  List<Map<String,Object>>  searchPosition(@PathVariable("keyword") String  keyword,@PathVariable("pageNo")int pageNo,
                                                     @PathVariable("pageSize")int  pageSize)throws  IOException{
        List<Map<String,Object>> list = service.searchPos(keyword, pageNo, pageSize);
        return  list;
    }

    @GetMapping("/searchSubject/{keyword}/{pageNo}/{pageSize}")
    @ResponseBody
    public  List<Map<String,Object>>  searchSubject(@PathVariable("keyword") String  keyword,@PathVariable("pageNo")int pageNo,
                                                     @PathVariable("pageSize")int  pageSize)throws  IOException{
        List<Map<String,Object>> list = service.searchSubject(keyword, pageNo, pageSize);
        list.stream()
                .forEach(map -> {
                    if (map.containsKey("sub_title")) {
                        Object value = map.get("sub_title");
                        if (value instanceof String) {
                            String replacedValue = ((String) value).replace("\\n", "");
                            map.put("sub_title", replacedValue);
                        }
                    }
                });
        return  list;
    }

}
