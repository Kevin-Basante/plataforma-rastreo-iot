import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BatteryWarning, Gauge, Info, Link2, MapPin, PawPrint, RadioTower, Settings2, Watch, Wifi, WifiOff } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Bateria, Campo, Cargando, EstadoConexion, Modal, Vacio } from '../componentes/Ui';
import { hace, TIPOS_DISPOSITIVO } from '../utilidades/formato';

const ICONO_DISPOSITIVO = { GPS: [RadioTower, 'vehiculo'], PULSERA: [Watch, 'familiar'], COLLAR: [PawPrint, 'mascota'] };

/** Estado de conexión y batería (HU-06) y configuración de límite de velocidad (HU-15). */
export default function Dispositivos() {
  const { usuario } = useSesion();
  const [lista, setLista] = useState(null);
  const [error, setError] = useState(null);
  const [editando, setEditando] = useState(null);
  const hogar = usuario.rol === 'USUARIO_HOGAR_B2C';

  const cargar = useCallback(() => {
    api.get('/api/dispositivos').then(setLista).catch(setError);
  }, []);

  useEffect(() => {
    cargar();
    const t = setInterval(cargar, 15000);
    return () => clearInterval(t);
  }, [cargar]);

  const conectados = lista?.filter((d) => d.conectado).length ?? 0;
  const bateriaBaja = lista?.filter((d) => d.bateriaBaja) ?? [];

  return (
    <Pagina titulo={hogar ? 'Mis dispositivos' : 'Dispositivos'}
      subtitulo={hogar ? 'Conexión y batería de los dispositivos enlazados a tu perfil' : 'Conexión y batería de los dispositivos de la flota'}
      acciones={<Link className="btn btn--primary" to="/dispositivos/enlazar"><Link2 />Enlazar dispositivo</Link>}>
      <div className="pg-06">
        <AvisoError error={error} />
        {!lista ? <Cargando /> : lista.length === 0 ? (
          <Vacio titulo="Aún no tienes dispositivos enlazados">Enlaza un GPS, una pulsera o un collar con su código QR o su serial.</Vacio>
        ) : (
          <div className="stack-6">
            <div className="resumen">
              <div><span className="type-icon"><RadioTower /></span><div><b>{lista.length}</b><br /><span>Dispositivos enlazados</span></div></div>
              <div><span className="type-icon" style={{ background: 'var(--color-success-soft)', color: 'var(--color-success)' }}><Wifi /></span><div><b>{conectados}</b><br /><span>Conectados</span></div></div>
              <div><span className="type-icon"><WifiOff /></span><div><b>{lista.length - conectados}</b><br /><span>Desconectados</span></div></div>
              <div><span className="type-icon" style={{ background: 'var(--color-warning-soft)', color: 'var(--color-warning)' }}><BatteryWarning /></span><div><b>{bateriaBaja.length}</b><br /><span>Con batería baja</span></div></div>
            </div>

            {bateriaBaja.map((d) => (
              <div key={d.id} className="banner banner--warning">
                <BatteryWarning />
                <div className="banner__body">
                  <b>Batería baja en {TIPOS_DISPOSITIVO[d.tipo] === 'GPS' ? 'el GPS' : `la ${TIPOS_DISPOSITIVO[d.tipo]}`} de {d.activoEtiqueta || d.serial} ({d.nivelBateria} %).</b>
                  <p>Recárgala hoy para no perder su ubicación ni sus alertas SOS.</p>
                </div>
              </div>
            ))}

            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Dispositivo</th><th>Activo monitoreado</th><th>Conexión</th><th>Batería estimada</th><th>Límite de velocidad</th><th /></tr></thead>
                <tbody>
                  {lista.map((d) => {
                    const [Icono, clase] = ICONO_DISPOSITIVO[d.tipo];
                    return (
                      <tr key={d.id} className={d.bateriaBaja ? 'is-flagged' : ''}>
                        <td><div className="cell-main"><span className={`type-icon type-icon--${clase}`}><Icono /></span><div><b>{TIPOS_DISPOSITIVO[d.tipo]}</b><span className="mono">{d.serial}</span></div></div></td>
                        <td><div className="cell-main"><div>
                          {d.activoId ? <><b className={d.activoTipo === 'VEHICULO' ? 'mono' : ''}>{d.activoEtiqueta}</b><span>{d.activoDescripcion}</span></>
                            : <><b className="muted">Sin activo asignado</b><span>Asígnalo desde Configurar</span></>}
                          {!hogar && d.nombreUsuario && <span>Responsable: {d.nombreUsuario}</span>}
                        </div></div></td>
                        <td><div className="conexion">
                          <EstadoConexion conectado={d.conectado} />
                          <small>{d.conectado ? `Último dato ${hace(d.ultimaConexion)}` : d.ultimaConexion ? `Última conexión ${hace(d.ultimaConexion)}` : 'Nunca ha reportado datos'}</small>
                          {!d.conectado && <small>Revisa que el equipo esté encendido y tenga señal.</small>}
                        </div></td>
                        <td><div className="bat">
                          <Bateria nivel={d.nivelBateria} />
                          {d.bateriaBaja && d.conectado && <span className="badge badge--warning"><BatteryWarning />Batería baja: recárgala hoy</span>}
                          {!d.conectado && d.nivelBateria != null && <small className="muted">Último valor reportado</small>}
                        </div></td>
                        <td>{d.limiteVelocidad
                          ? <span className="mono">{d.limiteVelocidad} km/h</span>
                          : <span className="badge badge--warning"><Gauge />Configuración pendiente</span>}</td>
                        <td className="num">
                          <div className="row" style={{ justifyContent: 'flex-end' }}>
                            <button className="btn btn--ghost btn--sm" onClick={() => setEditando(d)}><Settings2 />Configurar</button>
                            <Link className="btn btn--secondary btn--sm" to={`/monitoreo?dispositivo=${d.id}`}><MapPin />Ver en el mapa</Link>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <p className="nota"><Info />Un dispositivo aparece Desconectado cuando lleva más de 5 minutos sin enviar datos. La batería se marca como baja desde el 20 %.</p>
          </div>
        )}
        {editando && <ConfigurarDispositivo dispositivo={editando} onCerrar={() => setEditando(null)} onGuardado={() => { setEditando(null); cargar(); }} />}
      </div>
    </Pagina>
  );
}

function ConfigurarDispositivo({ dispositivo, onCerrar, onGuardado }) {
  const [activos, setActivos] = useState([]);
  const [activoId, setActivoId] = useState(dispositivo.activoId || '');
  const [limite, setLimite] = useState(dispositivo.limiteVelocidad ?? '');
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => { api.get('/api/activos').then(setActivos).catch(setError); }, []);

  const guardar = async () => {
    setGuardando(true);
    setError(null);
    try {
      if ((activoId || null) !== (dispositivo.activoId || null)) {
        await api.put(`/api/dispositivos/${dispositivo.id}/activo`, { activoId: activoId || null });
      }
      const valor = limite === '' ? null : Number(limite);
      if (valor !== (dispositivo.limiteVelocidad ?? null)) {
        await api.put(`/api/dispositivos/${dispositivo.id}/limite-velocidad`, { limiteVelocidad: valor });
      }
      onGuardado();
    } catch (err) {
      setError(err);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Modal titulo={`Configurar ${dispositivo.serial}`} subtitulo="Activo monitoreado y límite de velocidad" icono={Settings2} onCerrar={onCerrar}
      pie={<><button className="btn btn--ghost" onClick={onCerrar}>Cancelar</button><button className="btn btn--primary" onClick={guardar} disabled={guardando}>Guardar</button></>}>
      <AvisoError error={error} />
      <Campo etiqueta="Activo monitoreado" ayuda="Vehículo, familiar o mascota que lleva este dispositivo.">
        <select className="select" value={activoId} onChange={(e) => setActivoId(e.target.value)}>
          <option value="">Sin activo asignado</option>
          {activos.map((a) => <option key={a.id} value={a.id}>{a.etiqueta} · {a.descripcion}</option>)}
        </select>
      </Campo>
      <Campo etiqueta="Límite de velocidad (km/h)" ayuda="Si lo dejas vacío no se generan alertas de exceso de velocidad (configuración pendiente).">
        <div className="input-wrap"><Gauge /><input className="input mono" type="number" min="5" max="250" value={limite} onChange={(e) => setLimite(e.target.value)} placeholder="Sin límite" /><span className="suffix">km/h</span></div>
      </Campo>
    </Modal>
  );
}
