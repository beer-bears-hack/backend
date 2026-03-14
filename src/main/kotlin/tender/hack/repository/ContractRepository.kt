package tender.hack.repository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import tender.hack.domain.entity.ContractEntity
import java.math.BigDecimal
import java.time.Instant

@Repository
class ContractRepository(
    private val jdbcClient: JdbcClient
) {
    fun count(): Long {
        return jdbcClient.sql("SELECT COUNT(*) FROM contracts")
            .query(Long::class.java)
            .single()
    }

    fun saveBatch(contracts: List<ContractEntity>) {
        if (contracts.isEmpty()) return

        for (contract in contracts) {
            jdbcClient.sql("""
                INSERT INTO contracts (
                    purchase_name, quantity, contract_id, purchase_type,
                    initial_contract_price, final_contract_price, discount_percent,
                    nds_rate, contract_date, customer_region, supplier_region,
                    cte_id, cte_name, unit_price
                ) VALUES (
                    :purchaseName, :quantity, :contractId, :purchaseType,
                    :initialContractPrice, :finalContractPrice, :discountPercent,
                    :ndsRate, :contractDate, :customerRegion, :supplierRegion,
                    :cteId, :cteName, :unitPrice
                )
            """.trimIndent())
                .param("purchaseName", contract.purchaseName)
                .param("quantity", contract.quantity)
                .param("contractId", contract.contractId)
                .param("purchaseType", contract.purchaseType)
                .param("initialContractPrice", contract.initialContractPrice)
                .param("finalContractPrice", contract.finalContractPrice)
                .param("discountPercent", contract.discountPercent)
                .param("ndsRate", contract.ndsRate)
                .param("contractDate", contract.contractDate)
                .param("customerRegion", contract.customerRegion)
                .param("supplierRegion", contract.supplierRegion)
                .param("cteId", contract.cteId)
                .param("cteName", contract.cteName)
                .param("unitPrice", contract.unitPrice)
                .update()
        }
    }
}
