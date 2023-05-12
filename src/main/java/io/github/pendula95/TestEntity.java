package io.github.pendula95;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "test_entity")
public class TestEntity implements Serializable {

	private static final long serialVersionUID = 5779956475158502269L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", updatable = false, nullable = false)
	private Integer id;
	@Column(name = "email", nullable = false, unique = true, length = 255)
	private String email;
	@Column(name = "name", nullable = false)
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
