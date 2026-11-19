import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { alExpirarSesion, api, guardarToken, obtenerToken } from '../api/cliente';

const SesionContexto = createContext(null);

export function ProveedorSesion({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(Boolean(obtenerToken()));

  const cerrarSesion = useCallback(() => {
    guardarToken(null);
    setUsuario(null);
  }, []);

  useEffect(() => {
    alExpirarSesion(cerrarSesion);
    if (!obtenerToken()) return;
    api.get('/api/usuarios/me')
      .then(setUsuario)
      .catch(cerrarSesion)
      .finally(() => setCargando(false));
  }, [cerrarSesion]);

  const iniciar = useCallback((respuesta) => {
    guardarToken(respuesta.token);
    setUsuario(respuesta.usuario);
  }, []);

  const valor = useMemo(() => ({ usuario, setUsuario, cargando, iniciar, cerrarSesion }),
    [usuario, cargando, iniciar, cerrarSesion]);
  return <SesionContexto.Provider value={valor}>{children}</SesionContexto.Provider>;
}

export const useSesion = () => useContext(SesionContexto);
