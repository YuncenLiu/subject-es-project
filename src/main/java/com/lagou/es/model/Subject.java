package com.lagou.es.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @belongsProject: subject-es-project
 * @belongsPackage: com.lagou.es.model
 * @author: Xiang想
 * @createTime: 2024-07-10  11:24
 * @description: TODO
 * @version: 1.0
 */
@Data
@Table("s_subject")
@AllArgsConstructor
@NoArgsConstructor
public class Subject {
    @Id(keyType = KeyType.Auto)
    private Integer id;
    private Integer paperId;
    private String subNo;
    private String subTitle;
    private String subTag;
    private String subInfo;
    private String subRef;
}
