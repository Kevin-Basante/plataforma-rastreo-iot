package com.rastreoiot.datos;

import com.rastreoiot.modelo.Empresa;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmpresaRepository extends MongoRepository<Empresa, String> {

    Optional<Empresa> findByNit(String nit);
}
