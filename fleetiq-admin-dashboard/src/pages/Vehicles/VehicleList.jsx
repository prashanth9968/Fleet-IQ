import React, { useEffect, useState } from 'react';
import { Table } from '../../components/common/Table';
import { Card } from '../../components/common/Card';
import { Modal } from '../../components/common/Modal';
import { fetchService } from '../../services/api';
import { Plus, Edit2, Trash2 } from 'lucide-react';
import '../../styles/components.css';

export const VehicleList = () => {
  const [vehicles, setVehicles] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVehicle, setEditingVehicle] = useState(null);
  
  // Form fields
  const [regNum, setRegNum] = useState('');
  const [make, setMake] = useState('');
  const [model, setModel] = useState('');
  const [capacity, setCapacity] = useState('400');
  const [status, setStatus] = useState('ACTIVE');

  useEffect(() => {
    loadVehicles();
  }, []);

  const loadVehicles = async () => {
    const data = await fetchService('vehicle', 'vehicles');
    setVehicles(data || []);
  };

  const handleOpenAdd = () => {
    setEditingVehicle(null);
    setRegNum('');
    setMake('');
    setModel('');
    setCapacity('400');
    setStatus('ACTIVE');
    setIsModalOpen(true);
  };

  const handleOpenEdit = (v) => {
    setEditingVehicle(v);
    setRegNum(v.registrationNumber || v.regNum || '');
    setMake(v.make || '');
    setModel(v.model || '');
    setCapacity(v.fuelTankCapacityLitres ? v.fuelTankCapacityLitres.toString() : '400');
    setStatus(v.status || 'ACTIVE');
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const payload = {
      registrationNumber: regNum,
      make,
      model,
      fuelTankCapacityLitres: parseFloat(capacity),
      status,
      vehicleTypeId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11' // default template
    };

    if (editingVehicle) {
      await fetchService('vehicle', `vehicles/${editingVehicle.id || editingVehicle.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      });
      // Update local state fallback
      setVehicles(prev => prev.map(item => (item.id === editingVehicle.id ? { ...item, ...payload } : item)));
    } else {
      const newV = await fetchService('vehicle', 'vehicles', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      // Update local state fallback
      setVehicles(prev => [...prev, { id: 'v' + (prev.length + 1), ...payload }]);
    }

    setIsModalOpen(false);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this vehicle?')) {
      await fetchService('vehicle', `vehicles/${id}`, { method: 'DELETE' });
      setVehicles(prev => prev.filter(item => item.id !== id));
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '32px', margin: '0' }}>Vehicles Inventory</h1>
          <p style={{ color: 'var(--text-tertiary)' }}>Manage tracking devices, driver assignments, and fuel specifications</p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAdd}>
          <Plus size={16} /> Add Vehicle
        </button>
      </div>

      <Card>
        <Table
          headers={['Registration', 'Make & Model', 'Fuel Tank Capacity', 'Odometer Reading', 'Driver', 'Status', 'Actions']}
          data={vehicles}
          emptyMessage="No vehicles registered in this workspace."
          renderRow={(v) => (
            <tr key={v.id}>
              <td style={{ fontWeight: '600' }}>{v.registrationNumber || v.regNum}</td>
              <td>{v.make} {v.model}</td>
              <td>{v.fuelTankCapacityLitres || v.capacity} L</td>
              <td>{v.odometerReadingKm || 0} km</td>
              <td>{v.activeDriverName || <span style={{ color: 'var(--text-tertiary)' }}>Unassigned</span>}</td>
              <td>
                <span style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  fontSize: '11px',
                  fontWeight: '600',
                  backgroundColor: v.status === 'ACTIVE' ? 'var(--success-bg)' : 'var(--warning-bg)',
                  color: v.status === 'ACTIVE' ? 'var(--success)' : 'var(--warning)'
                }}>
                  {v.status}
                </span>
              </td>
              <td>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="btn btn-secondary" style={{ padding: '6px' }} onClick={() => handleOpenEdit(v)}>
                    <Edit2 size={14} />
                  </button>
                  <button className="btn btn-danger" style={{ padding: '6px' }} onClick={() => handleDelete(v.id)}>
                    <Trash2 size={14} />
                  </button>
                </div>
              </td>
            </tr>
          )}
        />
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingVehicle ? 'Edit Vehicle Details' : 'Register New Vehicle'}
      >
        <form onSubmit={handleSave}>
          <div className="input-group">
            <label className="input-label">Registration Number</label>
            <input
              type="text"
              className="input-field"
              value={regNum}
              onChange={(e) => setRegNum(e.target.value)}
              placeholder="e.g. KA-01-MJ-1024"
              required
            />
          </div>

          <div className="input-group">
            <label className="input-label">Manufacturer (Make)</label>
            <input
              type="text"
              className="input-field"
              value={make}
              onChange={(e) => setMake(e.target.value)}
              placeholder="e.g. TATA"
              required
            />
          </div>

          <div className="input-group">
            <label className="input-label">Model Name</label>
            <input
              type="text"
              className="input-field"
              value={model}
              onChange={(e) => setModel(e.target.value)}
              placeholder="e.g. Prima 4930.S"
              required
            />
          </div>

          <div className="input-group">
            <label className="input-label">Fuel Tank Capacity (Litres)</label>
            <input
              type="number"
              className="input-field"
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              required
            />
          </div>

          <div className="input-group">
            <label className="input-label">Status</label>
            <select className="input-field" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="IN_MAINTENANCE">Maintenance</option>
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
            <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              {editingVehicle ? 'Update Vehicle' : 'Register Vehicle'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
export default VehicleList;
