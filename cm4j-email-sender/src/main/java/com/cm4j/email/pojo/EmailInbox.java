package com.cm4j.email.pojo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * 收件箱 - 接受者邮件
 * 
 * @author yanghao
 * 
 */
@Entity
@Table(name = "email_inbox")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "email_inbox_sq", allocationSize = 1)
public class EmailInbox {

	/**
	 * 状态 - 禁用
	 */
	public static final String STATE_INVALID = "0";
	/**
	 * 状态 - 发送成功
	 */
	public static final String STATE_VALID = "1";
	/**
	 * 状态 - 待发送
	 */
	public static final String STATE_TO_SEND = "2";
	/**
	 * 状态 - 已发送，待验证
	 */
	public static final String STATE_TO_VERIFY = "3";
	/**
	 * 状态 - 发送失败
	 */
	public static final String STATE_IDENTITY_INVALID = "4";

	@Id
	@Column(name = "n_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
	private long id;

	@Column(name = "s_email", nullable = false)
	private String email;

	@Column(name = "s_state", nullable = false)
	private String state;

	@Column(name = "d_create", nullable = false)
	private Date createDate;

	@Column(name = "d_update", nullable = false)
	private Date updateDate;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

}
