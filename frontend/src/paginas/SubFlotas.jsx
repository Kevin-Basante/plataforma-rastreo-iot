import { useCallback, useEffect, useState } from 'react';
import { Car, Check, Layers, Plus, X } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Campo, Cargando, Denegado, Modal, Vacio } from '../componentes/Ui';
import { ROLES } from '../utilidades/formato';

/** Agrupación de vehículos en sub-flotas o sucursales (HU-18). */
export default function SubFlotas() {
  const { usuario } = useSesion();
  const [empresas, setEmpresas] = useState([]);
  const [empresaId, setEmpresaId] = useState(usuario.empresaId || '');
  const [subflotas, setSubflotas] = useState(null);
  const [vehiculos, setVehiculos] = useState([]);
  const [error, setError] = useState(null);
  const [denegado, setDenegado] = useState(null);
  const [creando, setCreando] = useState(false);
  const [agregando, setAgregando] = useState(null);
  const [nueva, setNueva] = useState(null);

  useEffect(() => {
    if (usuario.rol === 'SUPERADMIN') {
      api.get('/api/usuarios/empresas').then((l) => { setEmpresas(l); setEmpresaId((id) => id || l[0]?.id || ''); }).catch(setError);
    }
  }, [usuario.rol]);

  const cargar = useCallback(async () => {
    if (usuario.rol === 'SUPERADMIN' && !empresaId) return;
    const q = usuario.rol === 'SUPERADMIN' ? `?empresaId=${empresaId}` : '';
    try {
      const [s, v] = await Promise.all([api.get(`/api/subflotas${q}`), api.get(`/api/subflotas/vehiculos${q}`)]);
      setSubflotas(s);
      setVehiculos(v);
    } catch (err) {
      if (err.estado === 403) setDenegado(err.message); else setError(err);
    }
  }, [empresaId, usuario.rol]);

  useEffect(() => { cargar(); }, [cargar]);

  const agregar = async (sf, v) => {
    try {
      await api.put(`/api/subflotas/${sf.id}/vehiculos/${v.id}`);
      setNueva(`${sf.id}:${v.id}`);
      cargar();
    } catch (err) { setError(err); }
  };
  const quitar = async (sf, v) => {
    try { await api.del(`/api/subflotas/${sf.id}/vehiculos/${v.id}`); cargar(); } catch (err) { setError(err); }
  };

  if (denegado) {
    return (
      <Pagina titulo="Sub-flotas" subtitulo="Agrupa los vehículos de tu empresa por sucursal o tipo de operación">
        <Denegado titulo="Las sub-flotas son para clientes corporativos" detalle={denegado} rol={ROLES[usuario.rol].nombre}
          intento="Administrar sub-flotas" requiere="Ser Admin de Flota B2B o SuperAdmin" />
      </Pagina>
    );
  }

  const libres = vehiculos.filter((v) => v.subFlotas.length === 0);
  const empresaNombre = usuario.empresaNombre || empresas.find((e) => e.id === empresaId)?.nombre;

  return (
    <Pagina titulo="Sub-flotas" subtitulo={`Agrupa los vehículos de ${empresaNombre || 'la empresa'} por sucursal o tipo de operación`}
      acciones={<>
        {usuario.rol === 'SUPERADMIN' && (
          <select className="select" style={{ width: 240 }} value={empresaId} onChange={(e) => setEmpresaId(e.target.value)}>
            {empresas.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
          </select>
        )}
        <button className="btn btn--primary" onClick={() => setCreando(true)}><Plus />Nueva sub-flota</button>
      </>}>
      <div className="pg-13">
        {error && <div style={{ marginBottom: 'var(--sp-4)' }}><AvisoError error={error} /></div>}
        {!subflotas ? <Cargando /> : (
          <div className="sf">
            <section className="card">
              <div className="resumen-sf">
                <span><b>{subflotas.length}</b>sub-flotas</span>
                <span><b>{vehiculos.length}</b>vehículos en la empresa</span>
                <span><b>{libres.length}</b>sin agrupar</span>
              </div>
              {subflotas.length === 0 && <Vacio titulo="Aún no hay sub-flotas">Crea la primera agrupación con el botón Nueva sub-flota.</Vacio>}
              {subflotas.map((sf) => (
                <div key={sf.id} className="grupo">
                  <span className="type-icon"><Layers /></span>
                  <div><h3>{sf.nombre}<span>{sf.vehiculos.length} vehículos</span></h3>{sf.descripcion && <p className="small muted">{sf.descripcion}</p>}</div>
                  <button className="btn btn--secondary btn--sm" onClick={() => setAgregando(sf)}><Plus />Agregar vehículo</button>
                  <div className="placas">
                    {sf.vehiculos.map((v) => (
                      <span key={v.id} className={`placa${nueva === `${sf.id}:${v.id}` ? ' is-nueva' : ''}`}>
                        <Car />{v.placa}
                        <button className="quitar" title={`Quitar ${v.placa}`} onClick={() => quitar(sf, v)}><X /></button>
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </section>
            <aside className="card">
              <div className="card__head"><h3>Vehículos sin sub-flota</h3><span className="muted small">{libres.length}</span></div>
              <div className="card__body" style={{ paddingTop: 0, paddingBottom: 0 }}>
                {libres.length === 0 && <p className="small muted" style={{ padding: 'var(--sp-4) 0' }}>Todos los vehículos pertenecen al menos a una sub-flota.</p>}
                {libres.map((v) => (
                  <div key={v.id} className="libre">
                    <span className="type-icon type-icon--vehiculo"><Car /></span>
                    <div className="meta"><b>{v.placa}</b><span>{v.modelo}</span></div>
                  </div>
                ))}
              </div>
              <div className="card__foot"><span className="xs muted">Un vehículo puede pertenecer a varias sub-flotas.</span></div>
            </aside>
          </div>
        )}
        {creando && <CrearSubFlota empresaId={empresaId} vehiculos={vehiculos} onCerrar={() => setCreando(false)} onCreada={() => { setCreando(false); cargar(); }} />}
        {agregando && (
          <Modal titulo={`Agregar vehículo a ${agregando.nombre}`} icono={Car} onCerrar={() => setAgregando(null)}
            pie={<button className="btn btn--primary" onClick={() => setAgregando(null)}><Check />Listo</button>}>
            <div className="lista-veh">
              {vehiculos.map((v) => {
                const dentro = agregando.vehiculos.some((x) => x.id === v.id) || nueva === `${agregando.id}:${v.id}`;
                return (
                  <label key={v.id}>
                    <span className="check"><input type="checkbox" checked={dentro}
                      onChange={async () => { if (dentro) await quitar(agregando, v); else await agregar(agregando, v); setAgregando((s) => ({ ...s, vehiculos: dentro ? s.vehiculos.filter((x) => x.id !== v.id) : [...s.vehiculos, v] })); }} />
                      <span className="mono">{v.placa}</span> {v.modelo}</span>
                    <small>{v.subFlotas.length ? v.subFlotas.join(', ') : 'Sin sub-flota'}</small>
                  </label>
                );
              })}
            </div>
          </Modal>
        )}
      </div>
    </Pagina>
  );
}

function CrearSubFlota({ empresaId, vehiculos, onCerrar, onCreada }) {
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [seleccion, setSeleccion] = useState([]);
  const [error, setError] = useState(null);

  const crear = async () => {
    setError(null);
    try {
      await api.post('/api/subflotas', { nombre, descripcion, empresaId, vehiculoIds: seleccion });
      onCreada();
    } catch (err) { setError(err); }
  };
  const errorNombre = error?.detalles?.find((d) => d.includes('nombre'));

  return (
    <Modal titulo="Nueva sub-flota o sucursal" subtitulo="Agrupa vehículos para administrarlos en conjunto" icono={Layers} ancho onCerrar={onCerrar}
      pie={<><button className="btn btn--secondary" onClick={onCerrar}>Cancelar</button><button className="btn btn--primary" onClick={crear}><Check />Crear sub-flota</button></>}>
      <AvisoError error={error} />
      <div className="form-grid">
        <Campo etiqueta="Nombre" requerido error={errorNombre ? 'Escribe el nombre de la sub-flota.' : null}>
          <input className={`input${errorNombre ? ' is-error' : ''}`} value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Sucursal Sur" />
        </Campo>
        <Campo etiqueta="Descripción"><input className="input" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} placeholder="Zona o tipo de operación" /></Campo>
      </div>
      <div className="field">
        <label>Vehículos (opcional)</label>
        <div className="lista-veh">
          {vehiculos.map((v) => (
            <label key={v.id}>
              <span className="check"><input type="checkbox" checked={seleccion.includes(v.id)}
                onChange={(e) => setSeleccion(e.target.checked ? [...seleccion, v.id] : seleccion.filter((x) => x !== v.id))} />
                <span className="mono">{v.placa}</span> {v.modelo}</span>
              <small>{v.subFlotas.length ? v.subFlotas.join(', ') : 'Sin sub-flota'}</small>
            </label>
          ))}
        </div>
      </div>
    </Modal>
  );
}
