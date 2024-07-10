package com.lagou.es;

import com.lagou.es.mapper.SubjectMapper;
import com.lagou.es.model.Subject;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @belongsProject: subject-es-project
 * @belongsPackage: com.lagou.es
 * @author: Xiang想
 * @createTime: 2024-07-10  11:27
 * @description: TODO
 * @version: 1.0
 */
@SpringBootTest
public class DbTests {

    @Autowired
    private SubjectMapper subjectMapper;

    @Test
    public void contextLoads() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select();
        List<Subject> subjects = subjectMapper.selectListByQuery(queryWrapper);
        for (Subject subject : subjects) {
            System.out.println("subject = " + subject.toString());
        }
    }
}
