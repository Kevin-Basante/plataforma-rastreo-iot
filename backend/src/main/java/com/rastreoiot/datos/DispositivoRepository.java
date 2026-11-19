package com.rastreoiot.datos;

import com.rastreoiot.modelo.Dispositivo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DispositivoRepository extends MongoRepository<Dispositivo, String> {

    Optional<Dispositivo> findByCodigoQR(String codigoQR);

    Optional<Dispositivo> findBySerial(String serial);

    List<Dispositivo> findByUsuarioId(String usuarioId);

    List<Dispositivo> findByUsuarioIdIn(Collection<String> usuarioIds);

    Optional<Dispositivo> findByActivoId(String activoId);
}
