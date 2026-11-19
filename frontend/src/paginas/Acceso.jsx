import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Check, CircleCheck, Eye, EyeOff, LockKeyhole, Mail, ShieldCheck } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { AvisoError, Campo, Logo } from '../componentes/Ui';
import { CirculoGeocerca, Mapa, Marcador } from '../componentes/Mapa';

const REGISTRO_VACIO = { nombre: '', telefono: '', correo: '', contrasena: '', confirmacion: '', aceptaTerminos: false };

/** Registro e inicio de sesión seguro (HU-01). */
export default function Acceso() {
  const { usuario, iniciar } = useSesion();
  const navegar = useNavigate();
  const [vista, setVista] = useState('login');
  const [login, setLogin] = useState({ correo: '', contrasena: '' });
  const [registro, setRegistro] = useState(REGISTRO_VACIO);
  const [verClave, setVerClave] = useState(false);
  const [error, setError] = useState(null);
  const [erroresCampo, setErroresCampo] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [creado, setCreado] = useState(null);

  if (usuario && !creado) return <Navigate to="/monitoreo" replace />;

  const cambiarVista = (v) => { setVista(v); setError(null); setErroresCampo({}); };

  const enviarLogin = async (e) => {
    e.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      iniciar(await api.post('/api/auth/login', login));
      navegar('/monitoreo');
    } catch (err) {
      setError(err);
    } finally {
      setEnviando(false);
    }
  };

  const enviarRegistro = async (e) => {
    e.preventDefault();
    setError(null);
    const errores = {};
    if (!registro.nombre.trim()) errores.nombre = 'El nombre completo es obligatorio';
    if (!registro.telefono.trim()) errores.telefono = 'El teléfono celular es obligatorio';
    if (!registro.correo.trim()) errores.correo = 'El correo electrónico es obligatorio';
    if (!/^(?=.*[A-Z])(?=.*\d).{8,64}$/.test(registro.contrasena)) errores.contrasena = 'Mínimo 8 caracteres, una mayúscula y un número';
    if (registro.contrasena !== registro.confirmacion) errores.confirmacion = 'Las contraseñas no coinciden';
    if (!registro.aceptaTerminos) errores.terminos = 'Debes aceptar los términos y condiciones';
    setErroresCampo(errores);
    if (Object.keys(errores).length) return;
    setEnviando(true);
    try {
      const { confirmacion, ...datos } = registro;
      const respuesta = await api.post('/api/auth/registro', datos);
      setCreado(respuesta);
    } catch (err) {
      setError(err);
    } finally {
      setEnviando(false);
    }
  };

  const campoRegistro = (clave) => ({
    value: registro[clave],
    onChange: (e) => setRegistro({ ...registro, [clave]: e.target.value }),
    className: `input${erroresCampo[clave] ? ' is-error' : ''}`,
  });

  return (
    <div className="pg-01">
      <div className="acceso">
        <section className="marca">
          <Mapa centro={[4.65, -74.076]} zoom={15} zoomPos="topright">
            <CirculoGeocerca centro={[4.6488, -74.0645]} radio={320} detalle={false} />
            <Marcador posicion={[4.6512, -74.0791]} tipo="VEHICULO" etiqueta="KLM-482" seleccionado />
            <Marcador posicion={[4.6492, -74.0652]} tipo="FAMILIAR" etiqueta="Don Carlos" />
            <Marcador posicion={[4.6545, -74.069]} tipo="MASCOTA" etiqueta="Luna" />
          </Mapa>
          <div className="marca__velo" />
          <div className="marca__contenido">
            <div className="marca__logo"><Logo className="" /><span>Plataforma de Rastreo IoT</span></div>
            <div className="marca__texto">
              <h2>Tu vehículo, tu familia y tu mascota en un solo mapa.</h2>
              <p>Consulta dónde están, recibe alertas de geocerca y SOS, y revisa sus recorridos.</p>
              <div className="marca__lectura">
                <div><span>Latitud</span><b>4.65102</b></div>
                <div><span>Longitud</span><b>−74.08170</b></div>
                <div><span>Velocidad</span><b>48 km/h</b></div>
              </div>
            </div>
          </div>
        </section>

        <section className="formulario">
          <div className="tabs" style={{ marginBottom: 'var(--sp-8)' }}>
            <button type="button" className={`tab${vista === 'login' ? ' is-active' : ''}`} onClick={() => { setCreado(null); cambiarVista('login'); }}>Iniciar sesión</button>
            <button type="button" className={`tab${vista === 'registro' ? ' is-active' : ''}`} onClick={() => cambiarVista('registro')}>Crear cuenta</button>
          </div>

          {creado ? (
            <div className="exito">
              <div className="exito__icon"><CircleCheck /></div>
              <span className="badge badge--success"><Check />Cuenta creada</span>
              <h1>Te damos la bienvenida, {creado.usuario.nombre.split(' ')[0]}</h1>
              <p className="sub">Tu cuenta quedó lista y tu sesión ya está protegida con un token seguro. Empieza enlazando tu primer dispositivo.</p>
              <dl className="dl">
                <dt>Correo</dt><dd>{creado.usuario.correo}</dd>
                <dt>Rol</dt><dd>Usuario Hogar B2C</dd>
                <dt>Términos y condiciones</dt><dd>Aceptados</dd>
              </dl>
              <button className="btn btn--primary btn--lg btn--block" onClick={() => { iniciar(creado); navegar('/dispositivos/enlazar'); }}>Continuar</button>
            </div>
          ) : vista === 'login' ? (
            <>
              <h1>Inicia sesión</h1>
              <p className="sub">Usa el correo con el que te registraste.</p>
              <form onSubmit={enviarLogin}>
                <AvisoError error={error} />
                <div className="field">
                  <label htmlFor="correo">Correo electrónico</label>
                  <div className="input-wrap"><Mail />
                    <input id="correo" className={`input${error ? ' is-error' : ''}`} type="email" autoComplete="username" required
                      value={login.correo} onChange={(e) => setLogin({ ...login, correo: e.target.value })} placeholder="tu@correo.com" />
                  </div>
                </div>
                <div className="field">
                  <label htmlFor="clave">Contraseña</label>
                  <div className="input-wrap"><LockKeyhole />
                    <input id="clave" className={`input${error ? ' is-error' : ''}`} type={verClave ? 'text' : 'password'} autoComplete="current-password" required
                      value={login.contrasena} onChange={(e) => setLogin({ ...login, contrasena: e.target.value })} />
                    <button type="button" className="input-pass" onClick={() => setVerClave(!verClave)} aria-label="Mostrar contraseña">{verClave ? <EyeOff /> : <Eye />}</button>
                  </div>
                </div>
                <button className="btn btn--primary btn--lg btn--block" disabled={enviando}>{enviando ? 'Validando…' : 'Iniciar sesión'}</button>
              </form>
              <p className="pie">¿Aún no tienes cuenta? <a href="#registro" onClick={(e) => { e.preventDefault(); cambiarVista('registro'); }}>Crear cuenta</a></p>
            </>
          ) : (
            <>
              <h1>Crea tu cuenta</h1>
              <p className="sub">Todos los campos son obligatorios.</p>
              <form onSubmit={enviarRegistro} noValidate>
                <AvisoError error={error} />
                <div className="form-grid">
                  <Campo etiqueta="Nombre completo" requerido error={erroresCampo.nombre}><input {...campoRegistro('nombre')} autoComplete="name" /></Campo>
                  <Campo etiqueta="Teléfono celular" requerido error={erroresCampo.telefono}><input {...campoRegistro('telefono')} className={`${campoRegistro('telefono').className} mono`} autoComplete="tel" placeholder="310 456 7821" /></Campo>
                  <div className="span-2"><Campo etiqueta="Correo electrónico" requerido error={erroresCampo.correo}><input {...campoRegistro('correo')} type="email" autoComplete="email" /></Campo></div>
                  <Campo etiqueta="Contraseña" requerido error={erroresCampo.contrasena}><input {...campoRegistro('contrasena')} type="password" autoComplete="new-password" /></Campo>
                  <Campo etiqueta="Confirmar contraseña" requerido error={erroresCampo.confirmacion}><input {...campoRegistro('confirmacion')} type="password" autoComplete="new-password" /></Campo>
                  <span className="hint span-2 small muted" style={{ marginTop: -8 }}>Mínimo 8 caracteres, con al menos una mayúscula y un número.</span>
                </div>
                <label className="check">
                  <input type="checkbox" className={erroresCampo.terminos ? 'is-error' : ''} checked={registro.aceptaTerminos}
                    onChange={(e) => setRegistro({ ...registro, aceptaTerminos: e.target.checked })} />
                  <span>Acepto los términos y condiciones <span className="req" style={{ color: 'var(--color-danger)' }}>*</span></span>
                </label>
                {erroresCampo.terminos && <span className="error-msg small" style={{ color: 'var(--color-danger)' }}>{erroresCampo.terminos}</span>}
                <button className="btn btn--primary btn--lg btn--block" disabled={enviando}>{enviando ? 'Creando cuenta…' : 'Crear cuenta'}</button>
              </form>
              <p className="pie">¿Ya tienes cuenta? <a href="#login" onClick={(e) => { e.preventDefault(); cambiarVista('login'); }}>Inicia sesión</a></p>
            </>
          )}
          <div className="seguro"><ShieldCheck />Tu sesión se protege con un token seguro (JWT) que vence al cerrar sesión.</div>
        </section>
      </div>
    </div>
  );
}
