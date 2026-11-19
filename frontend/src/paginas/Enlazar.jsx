import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import jsQR from 'jsqr';
import { Camera, CircleCheck, Keyboard, Link2, PawPrint, RadioTower, ScanQrCode, Shapes, Watch } from 'lucide-react';
import { api } from '../api/cliente';
import { Pagina } from '../componentes/Layout';
import { AvisoError, EstadoConexion } from '../componentes/Ui';
import { TIPOS_DISPOSITIVO } from '../utilidades/formato';

/** Enlace de dispositivos por código QR o serial único (HU-04). */
export default function Enlazar() {
  const [pestana, setPestana] = useState('qr');
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const enlazar = async (datos) => {
    setError(null);
    setEnviando(true);
    try {
      setResultado(await api.post('/api/dispositivos/enlazar', datos));
    } catch (err) {
      setError(err);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Pagina titulo="Enlazar dispositivo" subtitulo="Asocia un GPS, una pulsera o un collar a tu perfil">
      <div className="pg-04">
        <div className="enlace">
          <section className="card">
            <div className="card__body" style={{ paddingBottom: 0 }}>
              <div className="tabs">
                <button className={`tab${pestana === 'qr' ? ' is-active' : ''}`} onClick={() => { setPestana('qr'); setResultado(null); setError(null); }}><ScanQrCode />Escanear código QR</button>
                <button className={`tab${pestana === 'serial' ? ' is-active' : ''}`} onClick={() => { setPestana('serial'); setResultado(null); setError(null); }}><Keyboard />Ingresar serial</button>
              </div>
            </div>
            <div className="card__body">
              {resultado ? <Resultado d={resultado} /> : pestana === 'qr'
                ? <EscanerQR onCodigo={(codigoQR) => enlazar({ codigoQR })} error={error} enviando={enviando} />
                : <FormSerial onEnviar={(serial) => enlazar({ serial })} error={error} enviando={enviando} />}
            </div>
          </section>

          <aside className="stack-6">
            <section className="card">
              <div className="card__head"><h3>Cómo enlazar</h3></div>
              <div className="card__body">
                <ol className="pasos">
                  <li><div><b>Enciende el dispositivo</b>Mantén presionado el botón lateral hasta que la luz parpadee.</div></li>
                  <li><div><b>Escanea el código o escribe el serial</b>Los dos están en la etiqueta posterior del equipo.</div></li>
                  <li><div><b>Asígnalo a un activo monitoreado</b>Un vehículo, un familiar o una mascota.</div></li>
                </ol>
              </div>
            </section>
            <section className="card">
              <div className="card__head"><h3>Dispositivos compatibles</h3></div>
              <div className="card__body" style={{ paddingTop: 'var(--sp-1)', paddingBottom: 'var(--sp-1)' }}>
                <div className="tipos">
                  <div className="list-item"><span className="type-icon type-icon--vehiculo"><RadioTower /></span><div className="meta"><b>GPS</b><span>Para vehículos</span></div><span className="mono xs muted">GPS-XXXX-XXXX</span></div>
                  <div className="list-item"><span className="type-icon type-icon--familiar"><Watch /></span><div className="meta"><b>Pulsera</b><span>Para familiares</span></div><span className="mono xs muted">PUL-XXXX-XXXX</span></div>
                  <div className="list-item"><span className="type-icon type-icon--mascota"><PawPrint /></span><div className="meta"><b>Collar</b><span>Para mascotas</span></div><span className="mono xs muted">COL-XXXX-XXXX</span></div>
                </div>
              </div>
            </section>
          </aside>
        </div>
      </div>
    </Pagina>
  );
}

function EscanerQR({ onCodigo, error, enviando }) {
  const video = useRef(null);
  const lienzo = useRef(null);
  const [estado, setEstado] = useState('inactiva');
  const [leido, setLeido] = useState(null);
  const [manual, setManual] = useState('');
  const alLeer = useRef(onCodigo);
  alLeer.current = onCodigo;

  useEffect(() => {
    if (estado !== 'activa') return undefined;
    let flujo;
    let cuadro;
    let detenido = false;
    navigator.mediaDevices?.getUserMedia({ video: { facingMode: 'environment' } })
      .then((s) => {
        flujo = s;
        if (detenido) { s.getTracks().forEach((t) => t.stop()); return; }
        video.current.srcObject = s;
        video.current.play();
        const leer = () => {
          if (detenido) return;
          const v = video.current;
          if (v && v.readyState === v.HAVE_ENOUGH_DATA) {
            const c = lienzo.current;
            c.width = v.videoWidth;
            c.height = v.videoHeight;
            const ctx = c.getContext('2d', { willReadFrequently: true });
            ctx.drawImage(v, 0, 0, c.width, c.height);
            const codigo = jsQR(ctx.getImageData(0, 0, c.width, c.height).data, c.width, c.height);
            if (codigo?.data) {
              setLeido(codigo.data);
              setEstado('leido');
              alLeer.current(codigo.data.trim());
              return;
            }
          }
          cuadro = requestAnimationFrame(leer);
        };
        leer();
      })
      .catch(() => setEstado('sin-camara'));
    return () => {
      detenido = true;
      cancelAnimationFrame(cuadro);
      flujo?.getTracks().forEach((t) => t.stop());
    };
  }, [estado]);

  return (
    <div className="stack">
      <AvisoError error={error} />
      <div className="camara">
        {estado === 'activa' && <span className="camara__vivo">Cámara activa</span>}
        <video ref={video} muted playsInline className="camara__video" style={{ display: estado === 'activa' ? 'block' : 'none' }} />
        <canvas ref={lienzo} hidden />
        {estado !== 'activa' && (
          <div className="camara__inicio">
            <button className="btn btn--primary btn--lg" onClick={() => { setLeido(null); setEstado('activa'); }}><Camera />{leido ? 'Escanear otro código' : 'Activar cámara'}</button>
            {estado === 'sin-camara' && <p>No pudimos acceder a la cámara. Escribe el código del QR abajo o usa la pestaña Ingresar serial.</p>}
          </div>
        )}
        {estado === 'activa' && <div className="visor"><i /><i /><i /><i /><b /></div>}
        {leido && (
          <div className="camara__estado">
            <span>Código detectado: <b className="mono">{leido}</b>. {enviando ? 'Validando que el dispositivo exista y esté disponible…' : ''}</span>
          </div>
        )}
      </div>
      <form className="row" onSubmit={(e) => { e.preventDefault(); if (manual.trim()) onCodigo(manual.trim()); }}>
        <input className="input mono" style={{ flex: 1 }} value={manual} onChange={(e) => setManual(e.target.value)} placeholder="O escribe el texto del código QR, por ejemplo RIOT-GPS-5E21-0B7C" />
        <button className="btn btn--secondary" disabled={enviando}>Validar código</button>
      </form>
      <p className="small muted">Centra el código dentro del recuadro. Si la cámara no lo lee, usa la pestaña Ingresar serial.</p>
    </div>
  );
}

function FormSerial({ onEnviar, error, enviando }) {
  const [serial, setSerial] = useState('');
  return (
    <form className="serial-form" onSubmit={(e) => { e.preventDefault(); onEnviar(serial); }}>
      <AvisoError error={error} />
      <div className="field">
        <label htmlFor="serial">Serial del dispositivo</label>
        <input id="serial" className={`input mono serial-input${error ? ' is-error' : ''}`} value={serial} autoFocus
          onChange={(e) => setSerial(e.target.value.toUpperCase())} placeholder="PUL-7F3A-21C9" />
        <span className="hint">Lo encuentras en la etiqueta posterior, debajo del código QR. Formato: TIPO-XXXX-XXXX.</span>
      </div>
      <div className="row">
        <button className="btn btn--primary btn--lg" disabled={enviando || !serial.trim()}><Link2 />Enlazar dispositivo</button>
        <Link className="btn btn--ghost" to="/dispositivos">Cancelar</Link>
      </div>
    </form>
  );
}

function Resultado({ d }) {
  const nombre = TIPOS_DISPOSITIVO[d.tipo];
  return (
    <div className="resultado">
      <div className="resultado__icon"><Link2 /></div>
      <span className="badge badge--success"><CircleCheck />Dispositivo enlazado</span>
      <h2>{d.tipo === 'GPS' ? 'El GPS' : `${nombre === 'Pulsera' ? 'La' : 'El'} ${nombre}`} ya está en tu perfil</h2>
      <p className="muted">Validamos el dispositivo y lo asociamos a tu cuenta. Ahora asígnalo al activo que va a llevarlo.</p>
      <dl className="dl">
        <dt>Tipo</dt><dd>{nombre}</dd>
        <dt>Serial</dt><dd className="mono">{d.serial}</dd>
        <dt>Conexión</dt><dd><EstadoConexion conectado={d.conectado} /></dd>
        <dt>Activo monitoreado</dt><dd className={d.activoEtiqueta ? '' : 'muted'}>{d.activoEtiqueta || 'Sin asignar'}</dd>
      </dl>
      <div className="row">
        <Link className="btn btn--primary" to={`/activos?dispositivo=${d.id}`}><Shapes />Registrar activo monitoreado</Link>
        <Link className="btn btn--secondary" to="/dispositivos">Ver mis dispositivos</Link>
      </div>
    </div>
  );
}
