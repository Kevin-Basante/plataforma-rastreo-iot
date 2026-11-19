import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CircleHelp, Crosshair, Gauge, Map as IconoMapa, MapPinOff, Power, PowerOff, Route, Satellite, TriangleAlert, Wifi, WifiOff } from 'lucide-react';
import { Polyline, CircleMarker, useMap } from 'react-leaflet';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Bateria, Cargando, IconoActivo } from '../componentes/Ui';
import { BOGOTA, CirculoGeocerca, Mapa, Marcador } from '../componentes/Mapa';
import { coordenada, hace, numero, TIPOS_DISPOSITIVO } from '../utilidades/formato';

/** Monitoreo en tiempo real: posición, velocidad, motor, kilometraje y vista del mapa (HU-06 a HU-09). */
export default function Monitoreo() {
  const { usuario } = useSesion();
  const [params, setParams] = useSearchParams();
  const [ubicaciones, setUbicaciones] = useState(null);
  const [geocercas, setGeocercas] = useState([]);
  const [traza, setTraza] = useState([]);
  const [capa, setCapa] = useState('ESTANDAR');
  const [avisoCapa, setAvisoCapa] = useState(null);
  const [toast, setToast] = useState(null);
  const [error, setError] = useState(null);
  const [encuadrar, setEncuadrar] = useState(0);
  const motores = useRef({});
  const seleccion = params.get('dispositivo');

  const cargar = useCallback(async () => {
    try {
      const lista = await api.get('/api/monitoreo/ubicaciones');
      // HU-08: aviso cuando la telemetría cambia el estado del motor.
      lista.forEach((u) => {
        const antes = motores.current[u.dispositivoId];
        if (antes !== undefined && u.motorEncendido != null && antes !== u.motorEncendido) {
          setToast({ texto: <>El motor de <b className="mono">{u.activoEtiqueta}</b> cambió a <b>{u.motorEncendido ? 'Encendido' : 'Apagado'}</b>. Kilometraje actualizado.</>, encendido: u.motorEncendido });
        }
        if (u.motorEncendido != null) motores.current[u.dispositivoId] = u.motorEncendido;
      });
      setUbicaciones(lista);
      setError(null);
    } catch (err) {
      setError(err);
    }
  }, []);

  useEffect(() => {
    cargar();
    api.get('/api/geocercas').then(setGeocercas).catch(() => {});
    const t = setInterval(cargar, 10000);
    return () => clearInterval(t);
  }, [cargar]);

  useEffect(() => { if (toast) { const t = setTimeout(() => setToast(null), 8000); return () => clearTimeout(t); } return undefined; }, [toast]);

  const seleccionado = useMemo(() => {
    if (!ubicaciones?.length) return null;
    return ubicaciones.find((u) => u.dispositivoId === seleccion)
      || ubicaciones.find((u) => u.conectado && u.ubicacionDisponible) || ubicaciones[0];
  }, [ubicaciones, seleccion]);

  // Últimos 15 minutos del activo seleccionado (traza en vivo).
  useEffect(() => {
    if (!seleccionado?.ubicacionDisponible) { setTraza([]); return; }
    const hasta = new Date();
    const desde = new Date(hasta.getTime() - 15 * 60000);
    api.get(`/api/monitoreo/dispositivos/${seleccionado.dispositivoId}/recorrido?desde=${desde.toISOString()}&hasta=${hasta.toISOString()}`)
      .then((r) => setTraza(r.puntos.map((p) => [p.latitud, p.longitud])))
      .catch(() => setTraza([]));
  }, [seleccionado?.dispositivoId, seleccionado?.fechaHora, seleccionado?.ubicacionDisponible]);

  // HU-09: valida la vista elegida con la API; si no existe o no carga, se mantiene la actual.
  const cambiarCapa = async (nueva) => {
    try {
      const { capa: valida } = await api.get(`/api/monitoreo/capas/${nueva}`);
      setCapa(valida);
      setAvisoCapa(null);
    } catch (err) {
      setAvisoCapa(err.message);
    }
  };
  const capaFallo = useCallback(() => {
    setCapa('ESTANDAR');
    setAvisoCapa('La vista satelital no respondió. Seguimos en la vista estándar y tus activos siguen visibles.');
  }, []);

  const seleccionar = (id) => setParams({ dispositivo: id });
  const empresa = usuario.empresaNombre;
  const subtitulo = usuario.rol === 'SUPERADMIN' ? 'Ubicación actual de todos los activos'
    : empresa ? `Ubicación actual de la flota de ${empresa}` : 'Ubicación actual de tus activos monitoreados';
  const conPosicion = ubicaciones?.filter((u) => u.ubicacionDisponible) || [];
  const enRuta = ubicaciones?.filter((u) => u.conectado && (u.velocidad ?? 0) > 0).length ?? 0;
  const geocercasSel = geocercas.filter((g) => g.dispositivoId === seleccionado?.dispositivoId);

  return (
    <Pagina titulo="Monitoreo en tiempo real" subtitulo={subtitulo} mapa>
      <div className="pg-07" style={{ position: 'absolute', inset: 0 }}>
        <Mapa centro={BOGOTA} zoom={13} capa={capa}>
          <DetectorFallo capa={capa} onFallo={capaFallo} />
          <Encuadre puntos={conPosicion.map((u) => [u.latitud, u.longitud])} clave={`${encuadrar}-${conPosicion.length > 0}`} />
          {seleccionado?.ubicacionDisponible && <Centrar posicion={[seleccionado.latitud, seleccionado.longitud]} id={seleccionado.dispositivoId} />}
          {geocercasSel.map((g) => <CirculoGeocerca key={g.id} centro={[g.latitudCentro, g.longitudCentro]} radio={g.radioMetros} nombre={g.nombre} estado={g.estado} detalle={false} />)}
          {traza.length > 1 && (<>
            <Polyline positions={traza} pathOptions={{ color: '#fff', weight: 8, opacity: 0.9, lineCap: 'round', lineJoin: 'round' }} />
            <Polyline positions={traza} pathOptions={{ color: '#0E7C66', weight: 4, opacity: 0.9, lineCap: 'round', lineJoin: 'round' }} />
            {traza.slice(0, -1).filter((_, i) => i % 3 === 0).map((p, i) => <CircleMarker key={i} center={p} radius={3.5} pathOptions={{ color: '#0E7C66', weight: 2, fillColor: '#fff', fillOpacity: 1 }} />)}
          </>)}
          {conPosicion.map((u) => (
            <Marcador key={u.dispositivoId} posicion={[u.latitud, u.longitud]} tipo={u.activoTipo}
              etiqueta={u.activoTipo === 'VEHICULO' && u.conectado ? `${u.activoEtiqueta}  ${numero(u.velocidad)} km/h` : u.activoEtiqueta.split(' ').slice(0, 2).join(' ')}
              seleccionado={u.dispositivoId === seleccionado?.dispositivoId} desconectado={!u.conectado}
              onClick={() => seleccionar(u.dispositivoId)} />
          ))}
        </Mapa>

        <div className="inspector">
          <section className="map-panel lista-activos">
            <div className="lista-activos__head">
              <h3>Activos monitoreados</h3>
              <span>{ubicaciones ? (ubicaciones.length > 5 ? `En ruta: ${enRuta} de ${ubicaciones.length}` : `${ubicaciones.length} en total`) : ''}</span>
            </div>
            <div className="list lista-scroll">
              {!ubicaciones && <Cargando />}
              {ubicaciones?.length === 0 && <p className="small muted" style={{ padding: 'var(--sp-4)' }}>Aún no tienes dispositivos enlazados. <Link to="/dispositivos/enlazar">Enlaza uno</Link>.</p>}
              {ubicaciones?.map((u) => (
                <button key={u.dispositivoId} className={`list-item clicable${u.dispositivoId === seleccionado?.dispositivoId ? ' is-selected' : ''}`} onClick={() => { seleccionar(u.dispositivoId); }}>
                  <IconoActivo tipo={u.activoTipo} />
                  <div className="meta">
                    <b className={u.activoTipo === 'VEHICULO' ? 'mono' : ''}>{u.activoEtiqueta}</b>
                    <span><i className={`estado-punto${u.conectado ? '' : ' is-off'}`} />{u.conectado ? 'Conectado' : 'Desconectado'} · {TIPOS_DISPOSITIVO[u.tipoDispositivo]}</span>
                  </div>
                  <div className="dato">
                    {u.conectado && u.ubicacionDisponible
                      ? <>{numero(u.velocidad)}<span className="unit">km/h</span><small>{hace(u.fechaHora)}</small></>
                      : <small>{u.ubicacionDisponible ? 'Última posición' : 'Sin ubicación'}</small>}
                  </div>
                </button>
              ))}
            </div>
          </section>
          {seleccionado && <Detalle u={seleccionado} geocercas={geocercasSel} />}
        </div>

        <div className="map-toolbar toolbar-vista">
          <div className="segmented">
            <button className={capa === 'ESTANDAR' ? 'is-active' : ''} onClick={() => cambiarCapa('ESTANDAR')}><IconoMapa />Estándar</button>
            <button className={capa === 'SATELITAL' ? 'is-active' : ''} onClick={() => cambiarCapa('SATELITAL')}><Satellite />Satélite</button>
            <button className="is-unavailable" title="Vista no disponible" onClick={() => cambiarCapa('RELIEVE')}><TriangleAlert className="alerta-mini" />Relieve</button>
          </div>
          <button className="btn btn--secondary btn--sm sombra" onClick={() => setEncuadrar((n) => n + 1)}><Crosshair />Ver todos</button>
        </div>
        {avisoCapa && (
          <div className="banner banner--warning map-overlay aviso-capa" style={{ boxShadow: 'var(--shadow-2)' }}>
            <TriangleAlert />
            <div className="banner__body"><b>Opción de mapa no disponible.</b><p>{avisoCapa}</p></div>
            <button className="btn btn--ghost btn--sm" onClick={() => setAvisoCapa(null)}>Entendido</button>
          </div>
        )}
        {error && <div className="map-overlay" style={{ right: 16, top: 70, maxWidth: 420 }}><AvisoError error={error} /></div>}
        {toast && <div className="toast float-toast aviso-motor">{toast.encendido ? <Power /> : <PowerOff />}<span>{toast.texto}</span></div>}
      </div>
    </Pagina>
  );
}

function Detalle({ u, geocercas }) {
  const vehiculo = u.activoTipo === 'VEHICULO';
  return (
    <section className="map-panel detalle">
      <div className="detalle__head">
        <IconoActivo tipo={u.activoTipo} />
        <div className="meta"><b className={vehiculo ? 'mono' : ''}>{u.activoEtiqueta}</b><span>{u.activoDescripcion}</span></div>
        {u.conectado ? <span className="badge badge--success"><Wifi />Conectado</span> : <span className="badge"><WifiOff />Desconectado</span>}
      </div>

      <div className="detalle__sec">
        <h4><Crosshair />Posición actual
          <span className="freshness"><span className={`pulse${u.conectado ? '' : ' is-off'}`} />{u.fechaHora ? `${u.conectado ? 'Actualizado' : 'Sin datos'} ${hace(u.fechaHora)}` : 'Sin datos'}</span>
        </h4>
        {u.ubicacionDisponible ? (
          <>
            {!u.conectado && <div className="no-disp"><MapPinOff /><div><b>Dispositivo desconectado</b><span>Se muestra la última posición conocida; la velocidad no se reporta hasta que vuelva la señal.</span></div></div>}
            <div className="readout" style={{ '--cols': 3 }}>
              <div className="readout__cell"><div className="readout__label">Latitud</div><div className="readout__value">{coordenada(u.latitud)}</div></div>
              <div className="readout__cell"><div className="readout__label">Longitud</div><div className="readout__value">{coordenada(u.longitud)}</div></div>
              <div className="readout__cell"><div className="readout__label">Velocidad</div>
                {u.velocidad != null ? <div className="readout__value">{numero(u.velocidad)}<span className="unit">km/h</span></div> : <div className="readout__value is-na">Sin dato</div>}
              </div>
            </div>
            {u.limiteVelocidad
              ? <span className="small muted">Límite configurado: {u.limiteVelocidad} km/h</span>
              : <span className="badge badge--warning" style={{ alignSelf: 'flex-start' }}><Gauge />Límite de velocidad: configuración pendiente</span>}
          </>
        ) : (
          <>
            <div className="no-disp"><MapPinOff /><div><b>Ubicación no disponible</b><span>El dispositivo no ha enviado coordenadas. No mostramos una posición que no podemos confirmar.</span></div></div>
            <div className="readout" style={{ '--cols': 3 }}>
              {['Latitud', 'Longitud', 'Velocidad'].map((t) => <div key={t} className="readout__cell"><div className="readout__label">{t}</div><div className="readout__value is-na">Sin dato</div></div>)}
            </div>
          </>
        )}
      </div>

      {vehiculo && (
        <div className="detalle__sec">
          <h4><Gauge />Motor y kilometraje</h4>
          {u.datosMotorDisponibles ? (
            <div className="readout" style={{ '--cols': 2 }}>
              <div className="readout__cell"><div className="readout__label">Estado del motor</div>
                <div style={{ marginTop: 6 }}><span className={`badge${u.motorEncendido ? ' badge--success' : ''}`}>{u.motorEncendido ? <Power /> : <PowerOff />}{u.motorEncendido ? 'Encendido' : 'Apagado'}</span></div>
              </div>
              <div className="readout__cell"><div className="readout__label">Kilometraje acumulado</div><div className="readout__value">{numero(u.kilometraje)}<span className="unit">km</span></div></div>
            </div>
          ) : (
            <div className="no-disp"><CircleHelp /><div><b>Dato del motor no disponible</b><span>No hay telemetría del motor ni del kilometraje. No mostramos valores que no podemos confirmar.</span></div></div>
          )}
        </div>
      )}

      <div className="detalle__sec">
        <dl className="dl">
          <dt>Dispositivo</dt><dd>{TIPOS_DISPOSITIVO[u.tipoDispositivo]} <span className="mono muted">{u.serial}</span></dd>
          <dt>Batería estimada</dt><dd><Bateria nivel={u.nivelBateria} /></dd>
          {geocercas.length > 0 && <><dt>Geocercas</dt><dd>{geocercas.map((g) => g.nombre).join(', ')}</dd></>}
        </dl>
        <div className="row">
          <Link className="btn btn--secondary btn--sm" to={`/recorrido?dispositivo=${u.dispositivoId}`}><Route />Ver recorrido</Link>
          {vehiculo && u.activoId && <Link className="btn btn--danger-outline btn--sm" to={`/control-motor/${u.activoId}`}><PowerOff />Control remoto del motor</Link>}
        </div>
      </div>
    </section>
  );
}

function Encuadre({ puntos, clave }) {
  const map = useMap();
  useEffect(() => {
    if (puntos.length > 1) map.fitBounds(puntos, { paddingTopLeft: [420, 80], paddingBottomRight: [120, 60], maxZoom: 15 });
    else if (puntos.length === 1) map.setView(puntos[0], 15);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, clave]);
  return null;
}

/** Centra el mapa al cambiar de activo seleccionado. */
function Centrar({ posicion, id }) {
  const map = useMap();
  const primero = useRef(true);
  useEffect(() => {
    if (primero.current) { primero.current = false; return; }
    map.flyTo(posicion, Math.max(map.getZoom(), 15), { duration: 0.6 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);
  return null;
}

/** Si la capa satelital no carga sus mosaicos, vuelve a la estándar (HU-09). */
function DetectorFallo({ capa, onFallo }) {
  const map = useMap();
  useEffect(() => {
    if (capa !== 'SATELITAL') return undefined;
    let errores = 0;
    const alError = () => { errores += 1; if (errores === 6) onFallo(); };
    map.eachLayer((l) => l.on?.('tileerror', alError));
    return () => map.eachLayer((l) => l.off?.('tileerror', alError));
  }, [map, capa, onFallo]);
  return null;
}
