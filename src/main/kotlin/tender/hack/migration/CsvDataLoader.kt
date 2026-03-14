package tender.hack.migration

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import tender.hack.domain.entity.CteEntity
import tender.hack.domain.entity.ContractEntity
import tender.hack.repository.ContractRepository
import tender.hack.repository.CteRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class CsvDataLoader(
    private val cteRepository: CteRepository,
    private val contractRepository: ContractRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(CsvDataLoader::class.java)

    private val cteCsvUrl = "https://storage.yandexcloud.net/hackathon-perm-2026/CTE.csv"
    private val contractsCsvUrl = "https://storage.yandexcloud.net/hackathon-perm-2026/contracts.csv"

    private val batchSize = 5000

    override fun run(args: ApplicationArguments) {
        log.info("Starting CSV data migration...")

        try {
            loadCteData()
            loadContractsData()
            log.info("CSV data migration completed successfully")
        } catch (e: Exception) {
            log.error("Error during CSV data migration", e)
            throw e
        }
    }

    private fun loadCteData() {
        val count = cteRepository.count()
        if (count > 0) {
            log.info("CTE table already has $count records, skipping migration")
            return
        }

        log.info("Starting CTE data migration from $cteCsvUrl")

        val reader = openHttpConnection(cteCsvUrl)

        try {
            val batch = mutableListOf<CteEntity>()
            var totalRows = 0L

            // Skip header
            reader.readLine()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val values = parseCsvLine(line!!)
                if (values.size >= 5) {
                    val cte = CteEntity(
                        id = 0,
                        cteId = values[0],
                        cteName = values[1],
                        category = values[2].ifEmpty { null },
                        manufacturer = values[3].ifEmpty { null },
                        characteristics = values[4].ifEmpty { null }
                    )
                    batch.add(cte)
                    totalRows++

                    if (batch.size >= batchSize) {
                        cteRepository.saveBatch(batch)
                        log.info("Processed $totalRows rows for cte...")
                        batch.clear()
                    }
                }
            }

            if (batch.isNotEmpty()) {
                cteRepository.saveBatch(batch)
                log.info("Processed $totalRows rows for cte...")
            }

            log.info("Successfully loaded $totalRows rows into cte")

        } finally {
            reader.close()
        }
    }

    private fun loadContractsData() {
        val count = contractRepository.count()
        if (count > 0) {
            log.info("Contracts table already has $count records, skipping migration")
            return
        }

        log.info("Starting Contracts data migration from $contractsCsvUrl")

        val reader = openHttpConnection(contractsCsvUrl)

        try {
            val batch = mutableListOf<ContractEntity>()
            var totalRows = 0L

            // Skip header
            reader.readLine()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val values = parseCsvLine(line!!)
                if (values.size >= 14) {
                    val contract = ContractEntity(
                        purchaseName = values[0],
                        quantity = values[1].toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        contractId = values[2],
                        purchaseType = values[3].ifEmpty { null },
                        initialContractPrice = values[4].toBigDecimalOrNull(),
                        finalContractPrice = values[5].toBigDecimalOrNull(),
                        discountPercent = values[6].toBigDecimalOrNull(),
                        ndsRate = values[7].ifEmpty { null },
                        contractDate = parseDate(values[8]),
                        customerRegion = values[9].ifEmpty { null },
                        supplierRegion = values[10].ifEmpty { null },
                        cteId = values[11],
                        cteName = values[12],
                        unitPrice = values[13].toBigDecimalOrNull() ?: BigDecimal.ZERO
                    )
                    batch.add(contract)
                    totalRows++

                    if (batch.size >= batchSize) {
                        contractRepository.saveBatch(batch)
                        log.info("Processed $totalRows rows for contracts...")
                        batch.clear()
                    }
                }
            }

            if (batch.isNotEmpty()) {
                contractRepository.saveBatch(batch)
                log.info("Processed $totalRows rows for contracts...")
            }

            log.info("Successfully loaded $totalRows rows into contracts")

        } finally {
            reader.close()
        }
    }

    private fun openHttpConnection(urlString: String): BufferedReader {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 60000
        connection.readTimeout = 300000
        connection.doInput = true

        return BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        result.add(current.toString())

        return result
    }

    private fun parseDate(dateStr: String): Instant? {
        return try {
            if (dateStr.isBlank()) return null
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            val localDateTime = LocalDateTime.parse(dateStr, formatter)
            localDateTime.toInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            null
        }
    }
}
