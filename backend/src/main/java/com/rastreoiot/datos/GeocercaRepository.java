package com.rastreoiot.datos;

import com.rastreoiot.modelo.Geocerca;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GeocercaRepository extends MongoRepository<Geocerca, String> {

    List<Geocerca> findByDispositivoId(String dispositivoId);

    List<Geocerca> findByDispositivoIdIn(Collection<String> dispositivoIds);
}
