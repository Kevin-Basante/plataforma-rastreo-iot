import { useEffect } from 'react';
import L from 'leaflet';
import { Circle, MapContainer, Marker, Polyline, TileLayer, useMap, ZoomControl } from 'react-leaflet';
import { renderToStaticMarkup } from 'react-dom/server';
import { ICONO_ACTIVO } from './Ui';
import { tipoActivoClave } from '../utilidades/formato';

export const BOGOTA = [4.651, -74.0817];

/** Capas disponibles (HU-09): vista vectorial estándar y satelital. */
export const CAPAS = {
  ESTANDAR: { url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', atribucion: '&copy; OpenStreetMap' },
  SATELITAL: { url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', atribucion: 'Imágenes &copy; Esri' },
};

export function Mapa({ centro = BOGOTA, zoom = 13, capa = 'ESTANDAR', children, className = 'map', zoomPos = 'bottomright' }) {
  const c = CAPAS[capa] || CAPAS.ESTANDAR;
  return (
    <MapContainer center={centro} zoom={zoom} zoomControl={false} className={className} attributionControl>
      <TileLayer key={capa} url={c.url} attribution={c.atribucion} maxZoom={19} />
      <ZoomControl position={zoomPos} />
      <SinPrefijo />
      {children}
    </MapContainer>
  );
}

function SinPrefijo() {
  const map = useMap();
  useEffect(() => { map.attributionControl?.setPrefix(false); }, [map]);
  return null;
}

/** Ajusta la vista para que todos los puntos sean visibles. */
export function Encuadre({ puntos, maxZoom = 16, dependencia }) {
  const map = useMap();
  useEffect(() => {
    const validos = puntos.filter((p) => p && p[0] != null && p[1] != null);
    if (validos.length === 1) map.setView(validos[0], Math.min(maxZoom, 15));
    else if (validos.length > 1) map.fitBounds(validos, { padding: [60, 60], maxZoom });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, dependencia]);
  return null;
}

export function VolarA({ posicion, zoom }) {
  const map = useMap();
  useEffect(() => {
    if (posicion && posicion[0] != null) map.flyTo(posicion, zoom || Math.max(map.getZoom(), 15), { duration: 0.6 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, posicion?.[0], posicion?.[1]]);
  return null;
}

export function Marcador({ posicion, tipo, etiqueta, seleccionado, desconectado, sos, onClick }) {
  const clave = tipoActivoClave(tipo);
  const Icono = ICONO_ACTIVO[clave];
  const clases = ['mk', `mk--${clave}`, seleccionado && 'is-selected', desconectado && 'mk--off', sos && 'mk--sos'].filter(Boolean).join(' ');
  const html = renderToStaticMarkup(
    <div className={clases}>
      {sos && <span className="mk__ring" />}
      <span className="mk__pin" />
      <Icono />
      {etiqueta && <span className="mk__label">{etiqueta}</span>}
    </div>,
  );
  const icon = L.divIcon({ className: '', html, iconSize: [36, 44], iconAnchor: [18, 43] });
  return <Marker position={posicion} icon={icon} zIndexOffset={seleccionado ? 1000 : 0} eventHandlers={onClick ? { click: onClick } : undefined} />;
}

export function Punto({ posicion, clase }) {
  const icon = L.divIcon({ className: '', html: `<div class="${clase}"></div>`, iconSize: [14, 14], iconAnchor: [7, 7] });
  return <Marker position={posicion} icon={icon} interactive={false} />;
}

/** Geocerca circular con su centro y radio visibles (HU-10). */
export function CirculoGeocerca({ centro, radio, nombre, estado = 'ACTIVA', detalle = true }) {
  const color = estado === 'INACTIVA' ? '#7C8B92' : estado === 'SILENCIADA' ? '#D98E04' : '#1F6FB2';
  const borde = [centro[0], centro[1] + radio / (111320 * Math.cos((centro[0] * Math.PI) / 180))];
  const medio = [centro[0], (centro[1] + borde[1]) / 2];
  const norte = [centro[0] + radio / 111320, centro[1]];
  const etiqueta = (html, w, h, ax, ay) => L.divIcon({ className: '', html, iconSize: [w, h], iconAnchor: [ax, ay] });
  return (
    <>
      <Circle center={centro} radius={radio} pathOptions={{ color, weight: 2, fillColor: color, fillOpacity: 0.12, dashArray: '6 6' }} />
      {detalle && (
        <>
          <Polyline positions={[centro, borde]} pathOptions={{ color, weight: 1.5 }} />
          <Marker position={centro} interactive={false} icon={etiqueta('<div class="mk-center"></div>', 12, 12, 6, 6)} />
          <Marker position={medio} interactive={false}
            icon={etiqueta(`<span class="radius-label">${Math.round(radio).toLocaleString('es-CO')} m</span>`, 60, 18, 30, 22)} />
        </>
      )}
      {nombre && (
        <Marker position={norte} interactive={false}
          icon={etiqueta(`<span class="geo-name">${nombre.replace(/[<>&"]/g, '')}</span>`, 140, 22, 70, 30)} />
      )}
    </>
  );
}
