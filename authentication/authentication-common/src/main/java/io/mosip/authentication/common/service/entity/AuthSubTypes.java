package io.mosip.authentication.common.service.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Sowmya
 *
 */
@NoArgsConstructor
@Data
@Table(name = "auth_sub_types", schema = "ida")
@Entity
public class AuthSubTypes {
	
	@NotNull
    @Column(name = "code")
    private String code;
	
	@NotNull
	@Column(name = "description")
    private String description;

	@NotNull
	@Column(name = "is_active")
	private boolean isActive;

	@NotNull
	@Column(name = "cr_by")
	private String crBy;

	@NotNull
	@Column(name = "cr_dtimes")
	private LocalDateTime crDTimes;

	@Column(name = "upd_by")
	private String updBy;

	@Column(name = "upd_dtimes")
	private LocalDateTime updDTimes;

}
