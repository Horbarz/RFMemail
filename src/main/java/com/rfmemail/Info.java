package com.rfmemail;




import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

public class Info {
	@NotNull
	@Email
	private String email;
	
	
	@NotNull
	private String to;
	
	@NotNull
	private String from;

	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	@Override
	public String toString() {
		return "Details [email=" + email + ", to=" + to + ", from=" + from + "]";
	}
	
	
	
	
	

}
