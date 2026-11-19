import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft, BatteryFull, BatteryLow, BatteryMedium, Car, CircleAlert, Lock, PawPrint, ShieldX, UserRound, Wifi, WifiOff, X,
} from 'lucide-react';
import { tipoActivoClave } from '../utilidades/formato';

export function Logo({ className = 'brand__mark' }) {
  return (
    <svg className={className} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="#0E7C66" />
      <circle cx="16" cy="16" r="10" fill="none" stroke="#fff" strokeOpacity=".45" strokeWidth="1.5" strokeDasharray="2.5 2.5" />
      <circle cx="16" cy="16" r="5" fill="none" stroke="#fff" strokeWidth="2" />
      <circle cx="16" cy="16" r="1.75" fill="#fff" />
      <path d="M16 3v4M16 25v4M3 16h4M25 16h4" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export function MarcaVacia() {
  return (
    <svg className="empty__mark" viewBox="0 0 56 56" fill="none" stroke="currentColor" aria-hidden="true">
      <circle cx="28" cy="28" r="22" strokeOpacity=".35" strokeWidth="1.5" strokeDasharray="3 4" />
      <circle cx="28" cy="28" r="11" strokeWidth="2" />
      <circle cx="28" cy="28" r="2.5" fill="currentColor" stroke="none" />
      <path d="M28 2v8M28 46v8M2 28h8M46 28h8" strokeWidth="1.75" strokeLinecap="round" />
    </svg>
  );
}

export const ICONO_ACTIVO = { vehiculo: Car, familiar: UserRound, mascota: PawPrint };

export function IconoActivo({ tipo, size }) {
  const clave = tipoActivoClave(tipo);
  const Icono = ICONO_ACTIVO[clave];
  return <span className={`type-icon type-icon--${clave}`} style={size ? { width: size, height: size } : undefined}><Icono /></span>;
}

export function Bateria({ nivel }) {
  if (nivel === null || nivel === undefined) return <span className="muted small">Sin dato</span>;
  const clase = nivel <= 20 ? 'battery--low' : nivel <= 50 ? 'battery--mid' : '';
  const Icono = nivel <= 20 ? BatteryLow : nivel <= 50 ? BatteryMedium : BatteryFull;
  return (
    <span className={`battery ${clase}`}>
      <Icono />
      <span className="battery__bar"><i style={{ width: `${nivel}%` }} /></span>
      <span className="battery__pct">{nivel} %</span>
    </span>
  );
}

export function EstadoConexion({ conectado }) {
  return conectado
    ? <span className="badge badge--success"><Wifi />Conectado</span>
    : <span className="badge"><WifiOff />Desconectado</span>;
}

export function Vacio({ titulo, children, centro = true }) {
  return (
    <div className={`empty${centro ? ' empty--center' : ''}`}>
      <MarcaVacia />
      <h3>{titulo}</h3>
      {children && <p>{children}</p>}
    </div>
  );
}

export function Cargando({ texto = 'Cargando…' }) {
  return <div className="cargando"><span className="spinner" />{texto}</div>;
}

/** Mensaje de error de formulario o de API (HU-01, HU-03, HU-05, HU-10...). */
export function AvisoError({ error }) {
  if (!error) return null;
  return (
    <div className="banner banner--danger" role="alert">
      <CircleAlert />
      <div className="banner__body">
        <b>{error.message}</b>
        {error.detalles?.length > 1 && (
          <ul className="lista-errores">{error.detalles.map((d) => <li key={d}>{d}</li>)}</ul>
        )}
      </div>
    </div>
  );
}

/** Pantalla de permisos insuficientes (HU-02, HU-14, HU-17). */
export function Denegado({ titulo, detalle, rol, intento, requiere, volver = '/monitoreo', textoVolver = 'Volver al monitoreo' }) {
  return (
    <div className="denied">
      <div className="denied__icon"><ShieldX /></div>
      <span className="badge badge--danger"><Lock />Permisos insuficientes</span>
      <h2>{titulo}</h2>
      <p>{detalle}</p>
      {rol && (
        <dl className="denied__meta">
          <dt>Tu rol</dt><dd>{rol}</dd>
          {intento && <><dt>Intentaste</dt><dd>{intento}</dd></>}
          {requiere && <><dt>Requiere</dt><dd>{requiere}</dd></>}
        </dl>
      )}
      <div className="row"><Link className="btn btn--primary" to={volver}><ArrowLeft />{textoVolver}</Link></div>
    </div>
  );
}

export function Modal({ titulo, subtitulo, icono: Icono, tono = 'primary', onCerrar, children, pie, ancho }) {
  useEffect(() => {
    const esc = (e) => e.key === 'Escape' && onCerrar?.();
    window.addEventListener('keydown', esc);
    return () => window.removeEventListener('keydown', esc);
  }, [onCerrar]);
  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onCerrar?.()}>
      <div className={`modal${ancho ? ' modal--wide' : ''}`} role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal__head">
          {Icono && <div className={`modal__icon modal__icon--${tono}`}><Icono /></div>}
          <div style={{ flex: 1 }}>
            <h2>{titulo}</h2>
            {subtitulo && <p>{subtitulo}</p>}
          </div>
          {onCerrar && <button className="modal__close" onClick={onCerrar} aria-label="Cerrar"><X /></button>}
        </div>
        <div className="modal__body">{children}</div>
        {pie && <div className="modal__foot">{pie}</div>}
      </div>
    </div>
  );
}

export function Campo({ etiqueta, requerido, error, ayuda, children }) {
  return (
    <div className="field">
      <label>{etiqueta}{requerido && <span className="req"> *</span>}</label>
      {children}
      {error ? <span className="error-msg"><CircleAlert />{error}</span> : ayuda ? <span className="hint">{ayuda}</span> : null}
    </div>
  );
}
