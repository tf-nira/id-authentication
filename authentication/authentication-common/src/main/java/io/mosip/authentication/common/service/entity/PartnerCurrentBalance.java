package io.mosip.authentication.common.service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
@Table(name = "partner_current_balance", schema = "ida")
@Entity
public class PartnerCurrentBalance {

	@Id
	@NotNull
	@Column(name = "partner_id")
	private String partnerId;

	@NotNull
	@Column(name = "balance", precision = 19, scale = 2)
	private BigDecimal balance;

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
