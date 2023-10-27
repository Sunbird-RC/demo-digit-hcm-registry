package org.egov.sunbird.Repository;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.data.query.builder.SelectQueryBuilder;
import org.egov.common.data.repository.GenericRepository;
import org.egov.common.producer.Producer;
import org.egov.sunbird.Constants;
import org.egov.sunbird.Repository.rowmapper.VcServiceDeliveryRowMapper;
import org.egov.sunbird.models.VcServiceDelivery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.egov.common.utils.CommonUtils.getIdMethod;

@Repository
@Slf4j
public class VCServiceDeliveryRepository extends GenericRepository<VcServiceDelivery> {

    protected VCServiceDeliveryRepository(Producer producer, NamedParameterJdbcTemplate namedParameterJdbcTemplate, RedisTemplate<String, Object> redisTemplate, SelectQueryBuilder selectQueryBuilder, VcServiceDeliveryRowMapper vcServiceDeliveryRowMapper , Optional<String> tableName) {
        super(producer, namedParameterJdbcTemplate, redisTemplate, selectQueryBuilder, vcServiceDeliveryRowMapper, Optional.of(Constants.NAME_OF_THE_MAPPER_TABLE));
    }

    public List<VcServiceDelivery> findById(List<String> ids, String columnName) {
        List<VcServiceDelivery> objFound = new ArrayList<>();

        String query = String.format("SELECT * FROM eg_vc_service_delivery vc WHERE vc.%s IN (:ids)", columnName);
        Map<String, Object> paramMap = new HashMap();
        paramMap.put("ids", ids);

        VcServiceDeliveryRowMapper vcServiceDeliveryRowMapper = new VcServiceDeliveryRowMapper();
        List<VcServiceDelivery> resultFromDB = this.namedParameterJdbcTemplate.query(query, paramMap,  vcServiceDeliveryRowMapper);
        Collections.reverse(resultFromDB);
        objFound.addAll(resultFromDB);
        putInCache(objFound);
        return objFound;
    }

}
