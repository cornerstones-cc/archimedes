package cc.cornerstones.arbutus.tinyid.persistence;

import cc.cornerstones.arbutus.tinyid.entity.TinyIdDo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class TinyIdDaoImpl implements TinyIdDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public TinyIdDo queryByBizType(String bizType) {
        String sql = "select id, biz_type, begin_id, max_id," +
                " step, delta, remainder, create_timestamp, last_update_timestamp, version" +
                " from t9_tiny_id where biz_type = ?";
        List<TinyIdDo> list = jdbcTemplate.query(sql, new Object[]{bizType}, new TinyIdDoRowMapper());
        if(list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public int updateMaxId(Long id, Long newMaxId, Long oldMaxId, Long version, String bizType) {
        String sql = "update t9_tiny_id set max_id= ?," +
                " last_update_timestamp=now(), version=version+1" +
                " where id=? and max_id=? and version=? and biz_type=?";
        return jdbcTemplate.update(sql, newMaxId, id, oldMaxId, version, bizType);
    }
    
    @Override
    public void create(TinyIdDo tinyIdDo){
        String sql = "insert into t9_tiny_id(biz_type, begin_id, max_id, step, delta, remainder, create_timestamp, " +
                "last_update_timestamp, version) " +
                "values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tinyIdDo.getBizType(), tinyIdDo.getBeginId(), tinyIdDo.getMaxId(),
                tinyIdDo.getStep(), tinyIdDo.getDelta(), tinyIdDo.getRemainder(), tinyIdDo.getCreateTimestamp(),
                tinyIdDo.getLastUpdateTimestamp(), tinyIdDo.getVersion());
    }

    public static class TinyIdDoRowMapper implements RowMapper<TinyIdDo> {

        @Override
        public TinyIdDo mapRow(ResultSet resultSet, int i) throws SQLException {
            TinyIdDo tinyIdDo = new TinyIdDo();
            tinyIdDo.setId(resultSet.getLong("id"));
            tinyIdDo.setBizType(resultSet.getString("biz_type"));
            tinyIdDo.setBeginId(resultSet.getLong("begin_id"));
            tinyIdDo.setMaxId(resultSet.getLong("max_id"));
            tinyIdDo.setStep(resultSet.getInt("step"));
            tinyIdDo.setDelta(resultSet.getInt("delta"));
            tinyIdDo.setRemainder(resultSet.getInt("remainder"));
            tinyIdDo.setCreateTimestamp(resultSet.getTimestamp("create_timestamp"));
            tinyIdDo.setLastUpdateTimestamp(resultSet.getTimestamp("last_update_timestamp"));
            tinyIdDo.setVersion(resultSet.getLong("version"));
            return tinyIdDo;
        }
    }
}
