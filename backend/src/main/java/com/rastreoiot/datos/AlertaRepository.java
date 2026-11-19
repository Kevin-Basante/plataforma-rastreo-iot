package com.rastreoiot.datos;

import com.rastreoiot.modelo.Alerta;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AlertaRepository extends MongoRepository<Alerta, String> {

    List<Alerta> findByDispositivoIdOrderByFechaHoraDesc(String dispositivoId);

    List<Alerta> findByDispositivoIdInOrderByFechaHoraDesc(Collection<String> dispositivoIds);

    List<Alerta> findAllByOrderByFechaHoraDesc();

    List<Alerta> findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraAsc(String dispositivoId, Instant desde, Instant hasta);
}
