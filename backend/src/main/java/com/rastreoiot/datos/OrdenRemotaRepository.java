package com.rastreoiot.datos;

import com.rastreoiot.modelo.OrdenRemota;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrdenRemotaRepository extends MongoRepository<OrdenRemota, String> {

    List<OrdenRemota> findByVehiculoIdOrderByFechaHoraDesc(String vehiculoId);
}
