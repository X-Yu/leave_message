package com.tonglukuaijian.commerce.dao.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.DigestUtils;

import com.tonglukuaijian.commerce.bean.User;
import com.tonglukuaijian.commerce.dao.UserDao;
import com.tonglukuaijian.commerce.mapper.UserMapper;

@Repository
public class UserDaoImpl implements UserDao {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Override
	public void save(User user) {
		jdbcTemplate.update(
				"insert into T_USERS(ACCOUNT_NUMBER,PASSWORD,NAME,PHONE_NUM,DEPARTMENT_ID,ROLE_ID,STATUS,CREATED_TIME) values(?,?,?,?,?,?,?,?)",
				new Object[] { user.getAccountNumber(), DigestUtils.md5DigestAsHex(user.getPassword().getBytes()),
						user.getName(), user.getPhoneNum(), user.getDepartmentId(), user.getRoleId(), user.getStatus(),
						user.getCreatedTime() });
	}

	@Override
	public void update(User user) {
		jdbcTemplate.update(
				"update T_USERS set PASSWORD=?,PHONE_NUM=?,DEPARTMENT_ID=?,ROLE_ID=?,STATUS=?,LOGIN_TIME=?, WHERE ID=?",
				new Object[] { DigestUtils.md5DigestAsHex(user.getPassword().getBytes()), user.getPhoneNum(),
						user.getDepartmentId(), user.getRoleId(), user.getStatus(), user.getLoginTime(),
						user.getId() });
	}

	@Override
	public User findById(Long userId) {
		String sql = "select * from T_USERS where ID=?";
		Object[] params = new Object[] { userId };
		User user = jdbcTemplate.queryForObject(sql, params, new UserMapper());
		return user;
	}

	@Override
	public User findByAccountNumberAndPassword(String accountNumber, String password) {
		String sql = "select * from T_USERS where ACCOUNT_NUMBER=? AND PASSWORD=?";
		Object[] params = new Object[] { accountNumber, password };
		User user = jdbcTemplate.queryForObject(sql, params, new UserMapper());
		return user;
	}

	@Override
	public List<User> findByParams(String accountNumber, String name, Long departmentId, Long roleId, String phoneNum,
			int page, int size) {
		String sql = "select * from T_USERS where 1=1";
		if (accountNumber != null) {
			sql += " and ACCOUNT_NUMBER = " + accountNumber;
		}
		if (null != phoneNum) {
			sql += " and PHONE_NUM like %" + phoneNum + "%";
		}
		if (name != null) {
			sql += " and NAME like %" + name + "%";
		}
		if (departmentId != null) {
			sql += " and DEPARTMENT_ID =" + departmentId;
		}
		if (roleId != null) {
			sql += " and ROLE_ID =" + roleId;
		}
		sql += " order by ID desc ";
		if (page > 0 && size > 0) {
			sql += " limit " + (page * size) + "," + size;
		}
		List<User> list = jdbcTemplate.query(sql, new UserMapper());
		return list;
	}
}
