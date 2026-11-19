package com.rastreoiot.datos;

import com.rastreoiot.modelo.Activo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivoRepository extends MongoRepository<Activo, String> {

    List<Activo> findByUsuarioId(String usuarioId);

    List<Activo> findByUsuarioIdIn(Collection<String> usuarioIds);
}
