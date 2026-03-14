package tender.hack.repository

import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.simple.JdbcClient.MappedQuerySpec

fun <T> MappedQuerySpec<T>.singleOrNull(): T? =
    DataAccessUtils.singleResult(list())
