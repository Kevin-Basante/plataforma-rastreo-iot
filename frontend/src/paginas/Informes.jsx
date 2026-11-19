import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CircleDashed, Download, FileText, Info, RefreshCw } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Cargando, Denegado, Logo } from '../componentes/Ui';
import { aInputLocal, deInputLocal, fechaHora, numero, ROLES, TIPOS_ALERTA, TIPOS_DISPOSITIVO } from '../utilidades/formato';

/** Exportación de informes PDF con distancia recorrida y eventos de alerta (HU-17). */
export default function Informes() {
  const { usuario } = useSesion();
  const [params, setParams] = useSearchParams();
  const [dispositivos, setDispositivos] = useState([]);
  const ahora = new Date();
  const [dispositivoId, setDispositivoId] = useState(params.get('dispositivo') || '');
  const [desde, setDesde] = useState(aInputLocal(new Date(ahora.getTime() - 7 * 86400000)));
  const [hasta, setHasta] = useState(aInputLocal(ahora));
  const [vista, setVista] = useState(null);
  const [error, setError] = useState(null);
  const [denegado, setDenegado] = useState(null);
  const [descargando, setDescargando] = useState(false);

  useEffect(() => {
    api.get('/api/dispositivos').then((l) => {
      setDispositivos(l);
      setDispositivoId((id) => id || (l.find((d) => d.tipo === 'GPS' && d.activoId) || l[0])?.id || '');
    }).catch(setError);
  }, []);

  const rango = () => `desde=${deInputLocal(desde)}&hasta=${deInputLocal(hasta)}`;

  const previsualizar = useCallback(async () => {
    if (!dispositivoId) return;
    setError(null);
    setDenegado(null);
    try {
      setVista(await api.get(`/api/monitoreo/dispositivos/${dispositivoId}/recorrido?${rango()}`));
      setParams({ dispositivo: dispositivoId });
    } catch (err) {
      if (err.estado === 403) setDenegado(err.message);
      else { setError(err); setVista(null); }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispositivoId]);

  useEffect(() => { previsualizar(); }, [previsualizar]);

  const exportar = async () => {
    setDescargando(true);
    setError(null);
    try {
      const blob = await api.blob(`/api/informes/dispositivos/${dispositivoId}/pdf?${rango()}`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = nombreArchivo();
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 2000);
    } catch (err) {
      if (err.estado === 403) setDenegado(err.message);
      else setError(err);
    } finally {
      setDescargando(false);
    }
  };

  const disp = dispositivos.find((d) => d.id === dispositivoId);
  const etiqueta = vista?.activoEtiqueta || disp?.serial || 'dispositivo';
  const nombreArchivo = () => `informe-${etiqueta.replace(/[^A-Za-z0-9-]/g, '_')}-${desde.slice(0, 10).replaceAll('-', '')}.pdf`;
  const eventos = vista?.eventos || [];
  const vacio = vista && !vista.hayDatos && eventos.length === 0;

  if (denegado) {
    return (
      <Pagina titulo="Informes PDF" subtitulo="Resumen de distancia recorrida y alertas de un dispositivo">
        <Denegado titulo="No puedes exportar este informe" detalle={denegado} rol={ROLES[usuario.rol].nombre}
          intento="Exportar el informe PDF de un dispositivo ajeno" requiere="Acceso al dispositivo o al informe" volver="/informes" textoVolver="Volver a Informes PDF" />
      </Pagina>
    );
  }

  return (
    <Pagina titulo="Informes PDF" subtitulo="Resumen de distancia recorrida y alertas de un dispositivo">
      <div className="pg-12">
        <div className="informes">
          <section className="card">
            <div className="card__head"><FileText /><h3>Generar informe</h3></div>
            <div className="card__body">
              <div className="field"><label>Dispositivo</label>
                <select className="select" value={dispositivoId} onChange={(e) => setDispositivoId(e.target.value)}>
                  {dispositivos.map((d) => <option key={d.id} value={d.id}>{TIPOS_DISPOSITIVO[d.tipo]} {d.serial}{d.activoEtiqueta ? ` · ${d.activoEtiqueta}` : ''}</option>)}
                </select>
              </div>
              <div className="field"><label>Desde</label><input type="datetime-local" className="input mono" value={desde} onChange={(e) => setDesde(e.target.value)} /></div>
              <div className="field"><label>Hasta</label><input type="datetime-local" className="input mono" value={hasta} onChange={(e) => setHasta(e.target.value)} /></div>
              <p className="xs muted">El informe incluye la distancia recorrida y los eventos de alerta del periodo.</p>
              <button className="btn btn--secondary" onClick={previsualizar}><RefreshCw />Actualizar vista previa</button>
            </div>
          </section>
          <section className="stack">
            <AvisoError error={error} />
            {vacio && <div className="banner banner--info"><Info /><div className="banner__body"><b>No hay registros en este rango.</b> El informe se genera igual, con un resumen vacío, y puedes exportarlo.</div></div>}
            <div className="visor">
              <div className="visor__barra">
                <FileText style={{ color: 'var(--color-danger)' }} />
                <span className="nombre">{nombreArchivo()}</span>
                <span className="muted small">Vista previa</span>
                <button className="btn btn--primary" onClick={exportar} disabled={!dispositivoId || descargando}><Download />{descargando ? 'Generando…' : 'Exportar PDF'}</button>
              </div>
              {!vista ? <Cargando /> : (
                <article className="hoja">
                  <div className="hoja__cab"><Logo className="" /><span className="prod">Plataforma de Rastreo IoT</span><span className="fecha">Generado el {fechaHora(new Date().toISOString())}<br />por {usuario.nombre}</span></div>
                  <h1>Informe de recorrido y alertas</h1>
                  <p className="sub">Periodo del {fechaHora(vista.desde)} al {fechaHora(vista.hasta)}</p>
                  <dl className="hoja__datos">
                    <div><dt>Dispositivo</dt><dd>{disp ? `${TIPOS_DISPOSITIVO[disp.tipo]} ${disp.serial}` : ''}</dd></div>
                    <div><dt>Activo monitoreado</dt><dd>{vista.activoEtiqueta}</dd></div>
                    <div><dt>Límite de velocidad</dt><dd>{disp?.limiteVelocidad ? `${disp.limiteVelocidad} km/h` : 'Configuración pendiente'}</dd></div>
                    <div><dt>Puntos registrados</dt><dd>{vista.puntos.length}</dd></div>
                  </dl>
                  <h2>Resumen</h2>
                  <div className="hoja__kpi">
                    <div><span>Distancia recorrida</span><b>{numero(vista.distanciaKm, 1)}<small>km</small></b></div>
                    <div><span>Velocidad máxima</span><b>{vista.velocidadMaxima != null ? numero(vista.velocidadMaxima) : '0'}<small>km/h</small></b></div>
                    <div><span>Alertas generadas</span><b>{eventos.length}</b></div>
                  </div>
                  <h2>Eventos de alerta</h2>
                  {eventos.length === 0 ? (
                    <div className="hoja__vacio"><CircleDashed /><div><b>Sin eventos de alerta en este periodo.</b>{vacio ? 'El dispositivo no envió posiciones ni generó alertas en el rango seleccionado.' : 'El recorrido no generó alertas.'}</div></div>
                  ) : (
                    <table>
                      <thead><tr><th>Fecha y hora</th><th>Tipo</th><th>Detalle</th></tr></thead>
                      <tbody>{eventos.map((a) => (
                        <tr key={a.id}><td className="mono">{fechaHora(a.fechaHora)}</td><td><span className={`tag tag--${a.tipo === 'EXCESO_VELOCIDAD' || a.tipo === 'SOS' ? 'v' : 'g'}`}>{TIPOS_ALERTA[a.tipo].texto}</span></td><td>{a.mensaje}</td></tr>
                      ))}</tbody>
                    </table>
                  )}
                  <div className="hoja__pie"><span>Plataforma de Rastreo IoT</span><span>Vista previa</span></div>
                </article>
              )}
            </div>
          </section>
        </div>
      </div>
    </Pagina>
  );
}
