package com.tonglukuaijian.commerce.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import com.tonglukuaijian.commerce.assembler.LeaveMessageAssembler;
import com.tonglukuaijian.commerce.bean.LeaveMessage;
import com.tonglukuaijian.commerce.bean.LeaveMessageAssignRecord;
import com.tonglukuaijian.commerce.bean.LeaveMessageFollow;
import com.tonglukuaijian.commerce.bean.LeaveMessageFollowRecord;
import com.tonglukuaijian.commerce.bean.Project;
import com.tonglukuaijian.commerce.bean.Role;
import com.tonglukuaijian.commerce.bean.User;
import com.tonglukuaijian.commerce.dao.LeaveMessageAssignRecordDao;
import com.tonglukuaijian.commerce.dao.LeaveMessageDao;
import com.tonglukuaijian.commerce.dao.LeaveMessageFollowDao;
import com.tonglukuaijian.commerce.dao.ProjectDao;
import com.tonglukuaijian.commerce.dao.RoleDao;
import com.tonglukuaijian.commerce.dao.UserDao;
import com.tonglukuaijian.commerce.dto.LeaveMessageAssignRecordDto;
import com.tonglukuaijian.commerce.dto.LeaveMessageInfo;
import com.tonglukuaijian.commerce.enums.LeaveMessageStatusEnum;
import com.tonglukuaijian.commerce.enums.RoleEnum;
import com.tonglukuaijian.commerce.exception.ServiceException;
import com.tonglukuaijian.commerce.service.LeaveMessageService;
import com.tonglukuaijian.commerce.vo.LeaveMessageAssignVo;
import com.tonglukuaijian.commerce.vo.LeaveMessageFollowVo;
import com.tonglukuaijian.commerce.vo.LeaveMessageVo;

@Service(value = "leaveMessageService")
public class LeaveMessageServiceImpl implements LeaveMessageService {
	Logger logger = LoggerFactory.getLogger(LeaveMessageServiceImpl.class);
	@Autowired
	private LeaveMessageDao leaveMessageDao;
	@Autowired
	private LeaveMessageAssignRecordDao leaveMessageAssignRecordDao;
	@Autowired
	private LeaveMessageFollowDao leaveMessageFollowDao;
	@Autowired
	private ProjectDao projectDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private RoleDao roleDao;

	@Override
	public void addLeaveMessage(LeaveMessageVo vo) {
		// 获取项目负责人
		Project project = projectDao.findProjectPrincipals(vo.getProjectId());
		LeaveMessage leaveMessage = wrapLeaveMessage(vo, project);
		leaveMessageDao.saveLeaveMesage(leaveMessage);
	}

	@Override
	public List<LeaveMessageInfo> getByParams(Long loginUserId, String projectId, String projectName,
			String principalName, String principalPhone, String customerName, Integer status, String createdTimeStart,
			String createdTimeEnd, int page, int size) {
		List<LeaveMessageInfo> result = new ArrayList<>();
		// 判断登录角色
		User user = userDao.findById(loginUserId);
		List<LeaveMessageInfo> list = new ArrayList<>();
		String endStaus = "";
		// 负责人
		Long principalUserId = loginUserId;
		// 所属者
		String beLongUserId = loginUserId + "";
		if (user.getRoleId() == RoleEnum.MINISTER.value()) {// 部长
			principalUserId = null;
			// 查看部门下面的所属着
			List<Project> ministerProjects = projectDao.findProjectByUserRelation(null, user.getId(), null, null, 0, 0);
			for (Project project : ministerProjects) {
				beLongUserId += "," + project.getPrincipalId();
			}
			endStaus += LeaveMessageStatusEnum.TODAY_COMMUNICATE + "," + LeaveMessageStatusEnum.NOT_CONTACT;
		} else if (user.getRoleId() == RoleEnum.GROUP.value()) { // 群总
			principalUserId = null;
			// 查看部门下面的所属着
			List<Project> ministerProjects = projectDao.findProjectByUserRelation(null, null, user.getId(), null, 0, 0);
			for (Project project : ministerProjects) {
				beLongUserId += "," + project.getPrincipalId();
			}
			endStaus += LeaveMessageStatusEnum.TODAY_COMMUNICATE + "," + LeaveMessageStatusEnum.NOT_CONTACT;
		} else if (user.getRoleId() == RoleEnum.PM.value()) { // 项目经理
			endStaus += LeaveMessageStatusEnum.TODAY_COMMUNICATE + "," + LeaveMessageStatusEnum.NOT_CONTACT;
		} else if (user.getRoleId() == RoleEnum.MEMBER.value()) { // 成员
			beLongUserId = null;
		}
		list = leaveMessageDao.findByParams(principalUserId, beLongUserId, projectId, projectName, principalName,
				principalPhone, customerName, endStaus, createdTimeStart, createdTimeEnd, page, size);

		for (LeaveMessageInfo leaveMessageDto : list) {
			leaveMessageDto.setCustomerPhone(encryption(leaveMessageDto.getCustomerPhone()));
			result.add(leaveMessageDto);
		}
		return result;
	}

	/**
	 * 手机号伪加密
	 * 
	 * @param phoneNum
	 * @return
	 */
	private String encryption(String phoneNum) {
		// 数据库解密
		String num = new String(Base64Utils.decodeFromString(phoneNum));
		char[] c = num.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (i > 3 && i < 8) {
				c[i] = '*';
			}
		}
		return new String(c);
	}

	private LeaveMessage wrapLeaveMessage(LeaveMessageVo vo, Project project) {
		LeaveMessage leaveMessage = new LeaveMessage();
		leaveMessage.setStatus(LeaveMessageStatusEnum.NORMAL.value());
		leaveMessage.setProjectId(vo.getProjectId());
		leaveMessage.setProjectName(vo.getProjectName());
		leaveMessage.setName(vo.getName());
		leaveMessage.setContent(vo.getContent());
		leaveMessage.setPhoneNum(Base64Utils.encodeToString(vo.getPhoneNum().getBytes()));
		leaveMessage.setCreatedTime(new Date());
		leaveMessage.setModifyTime(new Date());
		leaveMessage.setBelongToUserId(project.getPrincipalId());
		return leaveMessage;
	}

	@Override
	public LeaveMessageInfo getLeaveMessageInfo(Long loginUserId, Long leaveMessageId) {
		LeaveMessage leaveMessage = leaveMessageDao.findById(leaveMessageId);
		if (null == leaveMessage) {
			throw new ServiceException("该留言不存在");
		}
		User principalUser = userDao.findById(leaveMessage.getPrincipal());
		// 判断是否解密
		Boolean decode = false;
		if (loginUserId == leaveMessage.getPrincipal()) {
			decode = true;
		}
		LeaveMessageInfo dto = LeaveMessageAssembler.wrapLeaveMessageToLeaveMessageDto(leaveMessage, principalUser,
				decode);
		return dto;
	}

	@Override
	public void assignLeaveMessage(Long loginUserId,LeaveMessageAssignVo vo) {
		leaveMessageDao.assign(vo.getLeaveMessageId(), vo.getAssignUserId());

		User assignUser = userDao.findById(vo.getAssignUserId());
		Role assignRole = roleDao.findById(assignUser.getRoleId());
		LeaveMessage leaveMessage = leaveMessageDao.findById(vo.getLeaveMessageId());
		User operatorUser = userDao.findById(loginUserId);
		Role operatorRole = roleDao.findById(operatorUser.getRoleId());
		LeaveMessageAssignRecord po = wrapLeaveMessageAssignRecord(assignUser.getPhoneNum(), assignRole.getName(),
				assignUser.getAccountNumber(), assignUser.getName(), leaveMessage, operatorRole.getName(),
				operatorUser.getName(), operatorUser.getPhoneNum());
		leaveMessageAssignRecordDao.save(po);

	}

	/**
	 * 
	 * @param assignUserPhoneNum
	 * @param assignRoleName
	 * @param assignUserAccountNumber
	 * @param assignUserName
	 * @param leaveMessage
	 * @param operatorRoleName
	 * @param operatorUserName
	 * @param operatorUserPhone
	 * @return
	 */
	private LeaveMessageAssignRecord wrapLeaveMessageAssignRecord(String assignUserPhoneNum, String assignRoleName,
			String assignUserAccountNumber, String assignUserName, LeaveMessage leaveMessage, String operatorRoleName,
			String operatorUserName, String operatorUserPhone) {
		LeaveMessageAssignRecord po = new LeaveMessageAssignRecord();
		po.setAssignUserAccountNumber(assignUserAccountNumber);
		po.setAssignUserName(assignUserName);
		po.setAssignRoleName(assignRoleName);
		po.setAssignUserPhoneNum(assignUserPhoneNum);
		po.setCreatedTime(new Date());
		po.setLeaveMessageId(leaveMessage.getId());
		po.setLeaveMessageStatus(leaveMessage.getStatus());
		po.setOperatorRoleName(operatorRoleName);
		po.setOperatorUserName(operatorUserName);
		po.setOperatorUserPhone(operatorUserPhone);
		return po;
	}

	@Override
	public List<LeaveMessageAssignRecordDto> getLeaveMessageAssignRecordByParams(String projectId, String projectName,
			String projectPrincipal, String principalPhone, String customerName, Integer status,
			String createdTimeStart, String createdTimeEnd, int page, int size) {
		return leaveMessageAssignRecordDao.findByParams(projectId, projectName, projectPrincipal, principalPhone,
				customerName, status, createdTimeStart, createdTimeEnd, page, size);
	}

	@Override
	public List<LeaveMessageAssignRecord> getLeaveMessageAssignRecord(Long leaveMessageId) {
		return leaveMessageAssignRecordDao.findByLeaveMessageId(leaveMessageId);
	}

	@Override
	public LeaveMessageFollow getLeaveMessageFollowByLeaveMessageId(Long loginUserId, Long leaveMessageId) {
		LeaveMessageFollow follow = leaveMessageFollowDao.findByleaveMessageId(leaveMessageId);
		if (null != follow) {
			// 只能看到自己跟进
			if (follow.getOperatorUserId() != loginUserId) {
				return null;
			}
		}
		return follow;
	}

	@Override
	public LeaveMessageAssignRecord getLeaveMessageAssignRecordById(Long id) {
		LeaveMessageAssignRecord follow = leaveMessageAssignRecordDao.findById(id);
		return follow;
	}

	@Override
	public List<LeaveMessageFollowRecord> getFollowRecord(Long leaveMessageId, String createdTime) {
		List<LeaveMessageFollowRecord> list = leaveMessageFollowDao.findFollowRecord(leaveMessageId, createdTime);
		return list;
	}

	@Override
	public void followLeaveMessage(LeaveMessageFollowVo vo) {
		// 是否有跟进
		LeaveMessageFollow po = leaveMessageFollowDao.findByleaveMessageId(vo.getLeaveMessageId());
		if (null != po) {
			po = wrapLeaveMessageFollow(vo);
			leaveMessageFollowDao.update(po);
		} else {
			po = wrapLeaveMessageFollow(vo);
			po.setCreatedTime(new Date());
			leaveMessageFollowDao.save(po);
		}
		// 跟进日志
		User operatorUser = userDao.findById(vo.getOperatorUserId());
		if (operatorUser == null) {
			throw new ServiceException("用户不存在");
		}
		LeaveMessageFollowRecord followRecord = new LeaveMessageFollowRecord();
		followRecord.setCreatedTime(new Date());
		followRecord.setLeaveMessageId(vo.getLeaveMessageId());
		followRecord.setOperatorUserId(vo.getOperatorUserId());
		followRecord.setOperatorUserName(operatorUser.getName());
		followRecord.setRemark(vo.getRemark());
		followRecord.setReturnTime(vo.getReturnTime());
		followRecord.setStatus(vo.getStatus());
		leaveMessageFollowDao.saveFollowRecord(followRecord);
	}

	private LeaveMessageFollow wrapLeaveMessageFollow(LeaveMessageFollowVo vo) {
		LeaveMessageFollow lmf = new LeaveMessageFollow();
		lmf.setLeaveMessageId(vo.getLeaveMessageId());
		lmf.setModifyTime(new Date());
		lmf.setOperatorUserId(vo.getOperatorUserId());
		lmf.setRemark(vo.getRemark());
		lmf.setReturnTime(vo.getReturnTime());
		lmf.setStatus(vo.getStatus());
		return lmf;
	}

}
