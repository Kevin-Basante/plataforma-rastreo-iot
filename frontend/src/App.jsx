import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useSesion } from './contexto/Sesion';
import Layout from './componentes/Layout';
import { Cargando } from './componentes/Ui';
import Acceso from './paginas/Acceso';
import Monitoreo from './paginas/Monitoreo';
import Alertas from './paginas/Alertas';
import Geocercas from './paginas/Geocercas';
import Recorrido from './paginas/Recorrido';
import Dispositivos from './paginas/Dispositivos';
import Enlazar from './paginas/Enlazar';
import Activos from './paginas/Activos';
import SubFlotas from './paginas/SubFlotas';
import Informes from './paginas/Informes';
import ControlMotor from './paginas/ControlMotor';
import Usuarios from './paginas/Usuarios';
import Perfil from './paginas/Perfil';

/** Solo deja pasar a usuarios autenticados (HU-01); el resto va a la pantalla de acceso. */
function Protegida({ children }) {
  const { usuario, cargando } = useSesion();
  const ubicacion = useLocation();
  if (cargando) return <div style={{ height: '100vh', display: 'grid', placeItems: 'center' }}><Cargando texto="Verificando tu sesión…" /></div>;
  if (!usuario) return <Navigate to="/acceso" replace state={{ desde: ubicacion.pathname }} />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/acceso" element={<Acceso />} />
      <Route element={<Protegida><Layout /></Protegida>}>
        <Route path="/monitoreo" element={<Monitoreo />} />
        <Route path="/alertas" element={<Alertas />} />
        <Route path="/geocercas" element={<Geocercas />} />
        <Route path="/recorrido" element={<Recorrido />} />
        <Route path="/dispositivos" element={<Dispositivos />} />
        <Route path="/dispositivos/enlazar" element={<Enlazar />} />
        <Route path="/activos" element={<Activos />} />
        <Route path="/subflotas" element={<SubFlotas />} />
        <Route path="/informes" element={<Informes />} />
        <Route path="/control-motor" element={<ControlMotor />} />
        <Route path="/control-motor/:vehiculoId" element={<ControlMotor />} />
        <Route path="/usuarios" element={<Usuarios />} />
        <Route path="/perfil" element={<Perfil />} />
      </Route>
      <Route path="*" element={<Navigate to="/monitoreo" replace />} />
    </Routes>
  );
}
