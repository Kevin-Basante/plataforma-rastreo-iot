package com.rastreoiot.datos;

import com.rastreoiot.modelo.SubFlota;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubFlotaRepository extends MongoRepository<SubFlota, String> {

    List<SubFlota> findByEmpresaId(String empresaId);

    List<SubFlota> findByVehiculoIdsContaining(String vehiculoId);
}
