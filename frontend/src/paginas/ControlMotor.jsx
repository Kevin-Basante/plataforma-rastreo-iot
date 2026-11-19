import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Car, CircleCheck, CircleX, Clock, Gauge, History, Lock, Milestone, Power, PowerOff, RotateCw, Send, ShieldAlert, TriangleAlert, Wifi, WifiOff } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Cargando, Denegado, Modal, Vacio } from '../componentes/Ui';
import { Mapa, Marcador } from '../componentes/Mapa';
import { fechaHora, hace, numero, ROLES } from '../utilidades/formato';

const TIPOS = { APAGADO: ['Apagado', PowerOff, 'Apaga el motor. Úsala con el vehículo detenido.'], BLOQUEO: ['Bloqueo', Lock, 'Impide que el motor vuelva a encender.'] };

/** Apagado o bloqueo remoto del motor (HU-14). */
export default function ControlMotor() {
  const { vehiculoId } = useParams();
  const navegar = useNavigate();
  const { usuario } = useSesion();
  const [vehiculos, setVehiculos] = useState([]);
  const [vehiculo, setVehiculo] = useState(null);
  const [ubicacion, setUbicacion] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [tipo, setTipo] = useState('BLOQUEO');
  const [confirmar, setConfirmar] = useState(false);
  const [resultado, setResultado] = useState(null);
  const [denegado, setDenegado] = useState(null);
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    api.get('/api/activos').then((l) => {
      const v = l.filter((a) => a.tipo === 'VEHICULO');
      setVehiculos(v);
      if (!vehiculoId && v.length) navegar(`/control-motor/${v[0].id}`, { replace: true });
    }).catch(setError);
  }, [vehiculoId, navegar]);

  const cargar = useCallback(async () => {
    if (!vehiculoId) return;
    try {
      const v = await api.get(`/api/activos/${vehiculoId}`);
      setVehiculo(v);
      setDenegado(null);
      setHistorial(await api.get(`/api/ordenes/vehiculos/${vehiculoId}`));
      setUbicacion(v.dispositivoId ? await api.get(`/api/monitoreo/dispositivos/${v.dispositivoId}`) : null);
    } catch (err) {
      if (err.estado === 403) setDenegado({ mensaje: err.message });
      else setError(err);
    }
  }, [vehiculoId]);

  useEffect(() => { setResultado(null); setVehiculo(null); cargar(); }, [cargar]);

  const enviar = async () => {
    setConfirmar(false);
    setEnviando(true);
    setResultado(null);
    try {
      const orden = await api.post('/api/ordenes', { vehiculoId, tipo });
      setResultado({ ok: true, orden });
    } catch (err) {
      if (err.estado === 403) setDenegado({ mensaje: err.message, intento: true });
      else setResultado({ ok: false, mensaje: err.message });
    } finally {
      setEnviando(false);
      cargar();
    }
  };

  if (denegado) {
    return (
      <Pagina titulo="Control remoto del motor" subtitulo="Envía una orden de apagado o bloqueo al vehículo">
        <Denegado titulo="No puedes enviar órdenes remotas a este vehículo" detalle={`${denegado.mensaje} Solo los usuarios autorizados del vehículo pueden apagar o bloquear su motor.`}
          rol={ROLES[usuario.rol].nombre} intento={`Enviar una orden remota al vehículo ${vehiculoId}`} requiere="Ser usuario autorizado del vehículo" />
      </Pagina>
    );
  }

  const conectado = ubicacion?.conectado;
  const [nombreTipo, IconoTipo] = TIPOS[tipo];

  return (
    <Pagina titulo="Control remoto del motor" subtitulo="Envía una orden de apagado o bloqueo al vehículo"
      acciones={vehiculos.length > 1 && (
        <select className="select" style={{ width: 260 }} value={vehiculoId || ''} onChange={(e) => navegar(`/control-motor/${e.target.value}`)}>
          {vehiculos.map((v) => <option key={v.id} value={v.id}>{v.placa} · {v.modelo}</option>)}
        </select>
      )}>
      <div className="pg-10">
        {error && <AvisoError error={error} />}
        {!vehiculoId && vehiculos.length === 0 && <Vacio titulo="No tienes vehículos registrados">Registra un vehículo en Activos monitoreados.</Vacio>}
        {vehiculoId && !vehiculo && !error && <Cargando />}
        {vehiculo && (
          <div className="stack-6">
            {resultado?.ok && (
              <div className="banner banner--success"><CircleCheck />
                <div className="banner__body"><b>Orden de {TIPOS[resultado.orden.tipo][0]} enviada a {vehiculo.placa}.</b>
                  <p>{resultado.orden.detalle} El estado del motor se actualizará con el próximo dato de telemetría.</p></div>
              </div>
            )}
            {resultado && !resultado.ok && (
              <div className="banner banner--danger"><CircleX />
                <div className="banner__body"><b>No se pudo completar la orden: el vehículo no está disponible.</b><p>{resultado.mensaje}</p></div>
                <div className="banner__actions"><button className="btn btn--secondary btn--sm" onClick={() => setConfirmar(true)}><RotateCw />Reintentar</button></div>
              </div>
            )}
            <div className="vehiculo">
              <section className="card">
                <div className="ficha">
                  <span className="type-icon type-icon--vehiculo"><Car /></span>
                  <div className="meta"><h2>{vehiculo.placa}</h2><p>{vehiculo.modelo}{ubicacion ? <> · GPS <span className="mono">{ubicacion.serial}</span></> : ' · Sin GPS asociado'}</p></div>
                  {conectado ? <span className="badge badge--success"><Wifi />Conectado</span> : <span className="badge"><WifiOff />Sin estado válido</span>}
                </div>
                <div className="mapa-veh">
                  {ubicacion?.ubicacionDisponible
                    ? <Mapa key={vehiculo.id} centro={[ubicacion.latitud, ubicacion.longitud]} zoom={16}><Marcador posicion={[ubicacion.latitud, ubicacion.longitud]} tipo="VEHICULO" etiqueta={vehiculo.placa} seleccionado={conectado} desconectado={!conectado} /></Mapa>
                    : <div className="sin-mapa"><Vacio titulo="Ubicación no disponible">El vehículo no ha reportado coordenadas.</Vacio></div>}
                </div>
                <div className="card__body">
                  <div className="readout" style={{ '--cols': 4 }}>
                    <div className="readout__cell"><div className="readout__label"><Gauge />Velocidad</div>
                      {conectado && ubicacion?.velocidad != null ? <div className="readout__value">{numero(ubicacion.velocidad)}<span className="unit">km/h</span></div> : <div className="readout__value is-na">No disponible</div>}</div>
                    <div className="readout__cell"><div className="readout__label"><Power />Motor</div>
                      <div style={{ marginTop: 6 }}>{ubicacion?.datosMotorDisponibles
                        ? <span className={`badge${ubicacion.motorEncendido ? ' badge--success' : ''}`}>{ubicacion.motorEncendido ? <Power /> : <PowerOff />}{ubicacion.motorEncendido ? 'Encendido' : 'Apagado'}</span>
                        : <span className="readout__value is-na">No disponible</span>}</div></div>
                    <div className="readout__cell"><div className="readout__label"><Milestone />Kilometraje</div>
                      {ubicacion?.datosMotorDisponibles ? <div className="readout__value">{numero(ubicacion.kilometraje)}<span className="unit">km</span></div> : <div className="readout__value is-na">No disponible</div>}</div>
                    <div className="readout__cell"><div className="readout__label"><Clock />Último dato</div><div className="readout__value is-na">{ubicacion?.fechaHora ? hace(ubicacion.fechaHora) : 'Sin datos'}</div></div>
                  </div>
                </div>
              </section>
              <div className="stack-6">
                <section className="card zona-peligro control">
                  <div className="card__head"><ShieldAlert /><h3>Orden remota del motor</h3></div>
                  <div className="card__body">
                    <p className="small muted">Elige el tipo de orden. Se transmite al GPS del vehículo y queda registrada en el historial.</p>
                    <div className="orden">
                      {Object.entries(TIPOS).map(([k, [t, Icono, d]]) => (
                        <button key={k} type="button" className={`orden__op${tipo === k ? ' is-active' : ''}`} onClick={() => setTipo(k)}>
                          <span className="radio" /><b><Icono />{t}</b><span>{d}</span>
                        </button>
                      ))}
                    </div>
                    <button className="btn btn--danger btn--lg btn--block" disabled={enviando} onClick={() => setConfirmar(true)}>
                      {enviando ? <><Send />Enviando…</> : <><IconoTipo />{tipo === 'APAGADO' ? 'Apagar motor' : 'Bloquear motor'}</>}
                    </button>
                  </div>
                </section>
                <section className="card">
                  <div className="card__head"><History /><h3>Historial de órdenes remotas</h3></div>
                  {historial.length === 0 ? <p className="small muted" style={{ padding: 'var(--sp-4) var(--sp-5)' }}>Este vehículo aún no ha recibido órdenes remotas.</p> : (
                    <table className="table historial">
                      <thead><tr><th>Fecha y usuario</th><th>Tipo</th><th>Estado</th></tr></thead>
                      <tbody>
                        {historial.map((o, i) => {
                          const [t, Icono] = TIPOS[o.tipo];
                          return (
                            <tr key={o.id} style={i === 0 && resultado ? { background: resultado.ok ? '#F4FAF8' : '#FFF7F7' } : undefined} title={o.detalle}>
                              <td><span className="mono">{fechaHora(o.fechaHora)}</span><span className="xs muted" style={{ display: 'block' }}>{o.nombreUsuario}</span></td>
                              <td><span className="row" style={{ gap: 6 }}><Icono style={{ width: 15, height: 15 }} />{t}</span></td>
                              <td>{o.estado === 'ENVIADA' ? <span className="badge badge--primary"><Send />Enviada</span> : <span className="badge badge--danger"><CircleX />Fallida</span>}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </section>
              </div>
            </div>
          </div>
        )}
        {confirmar && vehiculo && (
          <Modal titulo={`¿Enviar orden de ${nombreTipo} a ${vehiculo.placa}?`} subtitulo={vehiculo.modelo} icono={IconoTipo} tono="danger" onCerrar={() => setConfirmar(false)}
            pie={<><button className="btn btn--secondary" onClick={() => setConfirmar(false)}>Cancelar</button><button className="btn btn--danger" onClick={enviar}><IconoTipo />{tipo === 'APAGADO' ? 'Apagar motor' : 'Bloquear motor'}</button></>}>
            {conectado && ubicacion?.velocidad > 0 && (
              <div className="banner banner--warning"><TriangleAlert /><div className="banner__body small"><b>El vehículo va a {numero(ubicacion.velocidad)} km/h.</b> La orden apaga el motor{tipo === 'BLOQUEO' ? ' y le impide volver a encender' : ''}.</div></div>
            )}
            <ul>
              <li>La orden se transmite al GPS {ubicacion ? <span className="mono">{ubicacion.serial}</span> : 'del vehículo'}.</li>
              <li>Queda registrada a nombre de {usuario.nombre}.</li>
            </ul>
          </Modal>
        )}
      </div>
    </Pagina>
  );
}

export function EnlaceControl({ id }) {
  return <Link className="btn btn--danger-outline btn--sm" to={`/control-motor/${id}`}><PowerOff />Control remoto del motor</Link>;
}
