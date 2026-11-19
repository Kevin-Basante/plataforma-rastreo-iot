package com.rastreoiot.datos;

import com.rastreoiot.modelo.LecturaTelemetria;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LecturaTelemetriaRepository extends MongoRepository<LecturaTelemetria, String> {

    List<LecturaTelemetria> findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraAsc(String dispositivoId, Instant desde, Instant hasta);

    Optional<LecturaTelemetria> findTopByDispositivoIdOrderByFechaHoraDesc(String dispositivoId);

    Optional<LecturaTelemetria> findTopByDispositivoIdAndLatitudNotNullOrderByFechaHoraDesc(String dispositivoId);

    void deleteByFechaHoraBefore(Instant limite);
}
