import { useEffect, useRef, useState } from 'react';
import { Bell, CheckCircle2, Gauge, IdCard, Image, LogIn, LogOut, Mail, Phone, Plus, Siren, Trash2, Upload } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Campo } from '../componentes/Ui';
import { iniciales, ROLES } from '../utilidades/formato';

const PREFERENCIAS = [
  ['ENTRADA_GEOCERCA', LogIn, 'Entrada a geocerca', 'Cuando un activo entra a una zona activa'],
  ['SALIDA_GEOCERCA', LogOut, 'Salida de geocerca', 'Cuando un activo sale de una zona activa'],
  ['SOS', Siren, 'SOS', 'Cuando alguien presiona el botón de pánico'],
  ['EXCESO_VELOCIDAD', Gauge, 'Exceso de velocidad', 'Cuando se supera el límite configurado'],
];
const TELEFONO = /^[0-9 +()-]{7,20}$/;

/** Actualización de datos personales, foto, contactos de emergencia y preferencias (HU-03). */
export default function Perfil() {
  const { usuario, setUsuario } = useSesion();
  const [form, setForm] = useState(() => copia(usuario));
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [errorFoto, setErrorFoto] = useState(null);
  const [nuevo, setNuevo] = useState({ nombre: '', parentesco: '', telefono: '' });
  const [errorNuevo, setErrorNuevo] = useState(null);
  const archivo = useRef(null);

  useEffect(() => {
    if (!aviso) return undefined;
    const t = setTimeout(() => setAviso(null), 3500);
    return () => clearTimeout(t);
  }, [aviso]);

  const cambiar = (campo) => (e) => setForm({ ...form, [campo]: e.target.value });

  const guardar = async (datos = form, mensaje = 'Tus datos se guardaron correctamente.') => {
    const e = {};
    if (!datos.nombre.trim()) e.nombre = 'Escribe tu nombre completo.';
    if (!datos.telefono.trim()) e.telefono = 'Escribe tu teléfono celular.';
    else if (!TELEFONO.test(datos.telefono.trim())) e.telefono = 'El teléfono celular no es válido. Usa solo números, por ejemplo 310 456 7821.';
    setErrores(e);
    setError(null);
    if (Object.keys(e).length) {
      setError({ message: 'No se guardaron los cambios. Revisa los campos marcados.' });
      return false;
    }
    try {
      const actualizado = await api.put('/api/usuarios/me', {
        nombre: datos.nombre.trim(), telefono: datos.telefono.trim(), ciudad: usuario.ciudad,
        contactosEmergencia: datos.contactosEmergencia, preferencias: datos.preferencias,
      });
      setUsuario(actualizado);
      setForm(copia(actualizado));
      setAviso(mensaje);
      return true;
    } catch (err) {
      setError(err);
      return false;
    }
  };

  const subirFoto = async (e) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (!f) return;
    setErrorFoto(null);
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(f.type)) { setErrorFoto({ message: 'La foto debe ser una imagen JPG, PNG o WEBP.' }); return; }
    if (f.size > 5 * 1024 * 1024) { setErrorFoto({ message: 'La foto no puede superar 5 MB.' }); return; }
    const datos = new FormData();
    datos.append('archivo', f);
    try {
      const actualizado = await api.put('/api/usuarios/me/foto', datos);
      setUsuario(actualizado);
      setAviso('Tu foto de perfil se actualizó.');
    } catch (err) { setErrorFoto(err); }
  };

  const agregarContacto = async () => {
    if (!nuevo.nombre.trim() || !nuevo.telefono.trim()) { setErrorNuevo('Escribe el nombre y el teléfono del contacto.'); return; }
    if (!TELEFONO.test(nuevo.telefono.trim())) { setErrorNuevo('El teléfono del contacto no es válido.'); return; }
    if (form.contactosEmergencia.length >= 5) { setErrorNuevo('Puedes registrar máximo 5 contactos de emergencia.'); return; }
    setErrorNuevo(null);
    const datos = { ...form, contactosEmergencia: [...form.contactosEmergencia, { nombre: nuevo.nombre.trim(), parentesco: nuevo.parentesco.trim(), telefono: nuevo.telefono.trim() }] };
    if (await guardar(datos, 'Contacto de emergencia agregado.')) setNuevo({ nombre: '', parentesco: '', telefono: '' });
  };

  const quitarContacto = (i) => guardar({ ...form, contactosEmergencia: form.contactosEmergencia.filter((_, j) => j !== i) }, 'Contacto de emergencia eliminado.');

  const cambiarPreferencia = (tipo, activa) => {
    const preferencias = PREFERENCIAS.map(([t]) => ({ tipoAlerta: t, activa: t === tipo ? activa : estaActiva(form.preferencias, t) }));
    guardar({ ...form, preferencias }, 'Preferencias de notificación actualizadas.');
  };

  return (
    <Pagina titulo="Mi perfil" subtitulo="Tus datos personales, contactos de emergencia y notificaciones">
      <div className="pg-03">
        {error && <div style={{ marginBottom: 'var(--sp-4)' }}><AvisoError error={error} /></div>}
        <div className="perfil">
          <div className="stack-6">
            <section className="card">
              <div className="card__head"><IdCard /><h3>Datos personales</h3></div>
              <div className="card__body">
                <div className="form-grid">
                  <div className="span-2"><Campo etiqueta="Nombre completo" requerido error={errores.nombre}>
                    <input className={`input${errores.nombre ? ' is-error' : ''}`} value={form.nombre} onChange={cambiar('nombre')} maxLength={80} />
                  </Campo></div>
                  <Campo etiqueta="Correo electrónico" ayuda="El correo es tu usuario de acceso y no se puede cambiar.">
                    <div className="input-wrap"><Mail /><input className="input" value={usuario.correo} disabled /></div>
                  </Campo>
                  <Campo etiqueta="Teléfono celular" requerido error={errores.telefono}>
                    <div className="input-wrap"><Phone /><input className={`input mono${errores.telefono ? ' is-error' : ''}`} value={form.telefono} onChange={cambiar('telefono')} /></div>
                  </Campo>
                  <Campo etiqueta="Rol"><input className="input" value={ROLES[usuario.rol].nombre} disabled /></Campo>
                  {usuario.empresaNombre && <Campo etiqueta="Empresa"><input className="input" value={usuario.empresaNombre} disabled /></Campo>}
                </div>
              </div>
              <div className="card__foot">
                <button className="btn btn--secondary" onClick={() => { setForm(copia(usuario)); setErrores({}); setError(null); }}>Descartar</button>
                <button className="btn btn--primary" onClick={() => guardar()}>Guardar cambios</button>
              </div>
            </section>

            <section className="card">
              <div className="card__head"><Siren /><h3>Contactos de emergencia</h3><span className="muted small">{form.contactosEmergencia.length} contactos</span></div>
              <div className="card__body">
                {form.contactosEmergencia.length === 0 && <p className="small muted">Aún no tienes contactos de emergencia.</p>}
                {form.contactosEmergencia.map((c, i) => (
                  <div key={`${c.nombre}-${i}`} className="contacto">
                    <div className="avatar">{iniciales(c.nombre)}</div>
                    <div className="meta"><b>{c.nombre}</b><span>{c.parentesco || 'Contacto'}</span></div>
                    <span className="mono small">{c.telefono}</span>
                    <button className="btn btn--ghost btn--sm" onClick={() => quitarContacto(i)}><Trash2 />Quitar</button>
                  </div>
                ))}
                <div className="nuevo-contacto">
                  <div className="field"><label>Nombre</label><input className="input" placeholder="Ej. Sofía Martínez" value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} /></div>
                  <div className="field"><label>Parentesco</label><input className="input" placeholder="Ej. Hija" value={nuevo.parentesco} onChange={(e) => setNuevo({ ...nuevo, parentesco: e.target.value })} /></div>
                  <div className="field"><label>Teléfono</label><input className="input mono" placeholder="3XX XXX XXXX" value={nuevo.telefono} onChange={(e) => setNuevo({ ...nuevo, telefono: e.target.value })} /></div>
                  <button className="btn btn--secondary" onClick={agregarContacto}><Plus />Agregar contacto</button>
                </div>
                {errorNuevo && <span className="error-msg" style={{ marginTop: 'var(--sp-2)' }}>{errorNuevo}</span>}
              </div>
            </section>
          </div>

          <div className="stack-6">
            <section className="card">
              <div className="card__head"><Image /><h3>Foto de perfil</h3></div>
              <div className="card__body stack">
                <div className="foto">
                  <div className="avatar avatar--lg">{usuario.fotoPerfil ? <img src={usuario.fotoPerfil} alt="Foto de perfil" /> : iniciales(usuario.nombre)}</div>
                  <div className="acciones">
                    <button className="btn btn--secondary btn--sm" onClick={() => archivo.current?.click()}><Upload />Cambiar foto</button>
                    <span className="xs muted">JPG, PNG o WEBP, hasta 5 MB.</span>
                    <input ref={archivo} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={subirFoto} />
                  </div>
                </div>
                <AvisoError error={errorFoto} />
              </div>
            </section>

            <section className="card">
              <div className="card__head"><Bell /><h3>Preferencias de notificación</h3></div>
              <div className="card__body" style={{ paddingTop: 'var(--sp-2)', paddingBottom: 'var(--sp-2)' }}>
                <p className="small muted" style={{ padding: 'var(--sp-2) 0 var(--sp-1)' }}>Elige qué tipos de alerta aparecen en la campana y en el Centro de alertas.</p>
                {PREFERENCIAS.map(([tipo, Icono, titulo, texto]) => (
                  <div key={tipo} className="pref">
                    <span className="type-icon" style={tipo === 'SOS' ? { background: 'var(--color-danger-soft)', color: 'var(--color-danger)' } : undefined}><Icono /></span>
                    <div className="meta"><b>{titulo}</b><span>{texto}</span></div>
                    <label className="switch"><input type="checkbox" aria-label={titulo} checked={estaActiva(form.preferencias, tipo)} onChange={(e) => cambiarPreferencia(tipo, e.target.checked)} /><span /></label>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </div>
        {aviso && <div className="toast-fijo"><div className="toast"><CheckCircle2 /><div>{aviso}</div></div></div>}
      </div>
    </Pagina>
  );
}

function copia(u) {
  return {
    nombre: u.nombre || '',
    telefono: u.telefono || '',
    contactosEmergencia: (u.contactosEmergencia || []).map((c) => ({ ...c })),
    preferencias: (u.preferencias || []).map((p) => ({ ...p })),
  };
}

function estaActiva(preferencias, tipo) {
  const p = preferencias.find((x) => x.tipoAlerta === tipo);
  return p ? p.activa : true;
}
