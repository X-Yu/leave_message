package com.tonglukuaijian.commerce.dao.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.tonglukuaijian.commerce.bean.Department;
import com.tonglukuaijian.commerce.dao.DepartmentDao;
import com.tonglukuaijian.commerce.mapper.DepartmentMapper;

@Repository
public class DepartmentDaoImpl implements DepartmentDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void save(Department department) {
		jdbcTemplate.update("insert into T_DEPARTMENT (`NAME`,`STATUS`,CREATED_TIME) values(?,?,?)",
				new Object[] { department.getName(), department.getStatus(), department.getCreatedTime() });
	}

	@Override
	public void update(Department department) {
		jdbcTemplate.update("update T_DEPARTMENT NAME=?,STATUS=? where ID=?",
				new Object[] { department.getName(), department.getStatus(), department.getId() });
	}

	@Override
	public List<Department> findAll() {
		String sql = "select * from T_DEPARTMENT";
		List<Department> list = jdbcTemplate.query(sql, new DepartmentMapper());
		return list;
	}

}
