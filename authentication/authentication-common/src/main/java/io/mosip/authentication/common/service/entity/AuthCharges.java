package io.mosip.authentication.common.service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author Sowmya
 *
 */
@Getter 
@Setter 
@ToString 
@Entity
@IdClass(AuthChargesPK.class)
@NoArgsConstructor
@Table(schema = "ida", name = "auth_charges")
public class AuthCharges {

	@Id
	@NotNull
    @Column(name = "type_code")
    private String typeCode;
	
	@Id
	@NotNull
    @Column(name = "sub_type_code")
    private String subTypeCode ;
	
	@NotNull
	@Column(name = "amount", precision = 19, scale = 2)
	private BigDecimal amount;

	@Id
	@NotNull
	@Column(name = "effective_from")
	private LocalDateTime effectiveFrom;

	@Column(name = "effective_to")
	private LocalDateTime effectiveTo;

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
