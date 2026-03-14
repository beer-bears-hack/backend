package tender.hack.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tender.hack.domain.entity.ContractEntity
import java.math.BigDecimal
import java.sql.Timestamp

@Repository
class ContractRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    fun count(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contracts", Long::class.java) ?: 0L
    }

    fun saveBatch(contracts: List<ContractEntity>) {
        if (contracts.isEmpty()) return

        val sql = """
            INSERT INTO contracts (
                purchase_name, quantity, contract_id, purchase_type,
                initial_contract_price, final_contract_price, discount_percent,
                nds_rate, contract_date, customer_region, supplier_region,
                cte_id, cte_name, unit_price
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        for (contract in contracts) {
            jdbcTemplate.update(sql) { ps ->
                ps.setString(1, contract.purchaseName)
                ps.setBigDecimal(2, contract.quantity)
                ps.setString(3, contract.contractId)
                ps.setString(4, contract.purchaseType)
                ps.setBigDecimal(5, contract.initialContractPrice)
                ps.setBigDecimal(6, contract.finalContractPrice)
                ps.setBigDecimal(7, contract.discountPercent)
                ps.setString(8, contract.ndsRate)
                
                if (contract.contractDate != null) {
                    ps.setTimestamp(9, Timestamp.from(contract.contractDate))
                } else {
                    ps.setNull(9, java.sql.Types.TIMESTAMP)
                }
                
                ps.setString(10, contract.customerRegion)
                ps.setString(11, contract.supplierRegion)
                ps.setString(12, contract.cteId)
                ps.setString(13, contract.cteName)
                ps.setBigDecimal(14, contract.unitPrice)
            }
        }
    }
}
