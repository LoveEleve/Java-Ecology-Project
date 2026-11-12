package com.loveeleve.ecology.mybatis.service;

import com.loveeleve.ecology.mybatis.entity.User;
import com.loveeleve.ecology.mybatis.mapper.UserMapper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserBatchService {

    private final SqlSessionFactory sqlSessionFactory;

    public UserBatchService(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void batchInsert(List<User> users) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            for (User user : users) {
                mapper.insert(user);
            }
            session.flushStatements();
            session.commit();
        }
    }
}
