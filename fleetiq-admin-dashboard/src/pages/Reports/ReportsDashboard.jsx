import React, { useState } from 'react';
import { Card } from '../../components/common/Card';
import { Table } from '../../components/common/Table';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { BarChart3, FileText, Download, Calendar, Play, Filter, Share2, Mail, MessageCircle, Link } from 'lucide-react';
import '../../styles/components.css';

export const ReportsDashboard = () => {
  const [reportType, setReportType] = useState('WEEKLY');
  const [startDate, setStartDate] = useState('2026-06-25');
  const [endDate, setEndDate] = useState('2026-07-01');
  const [format, setFormat] = useState('PDF');
  
  const [reports, setReports] = useState([
    { id: 'rep1', name: 'Weekly Operational KPI Summary', type: 'WEEKLY', period: '06/25/2026 - 07/01/2026', format: 'PDF', size: '1.2 MB', date: '2026-07-01' },
    { id: 'rep2', name: 'Daily Fleet Fuel Logistics Audit', type: 'DAILY', period: '06/30/2026', format: 'XLSX', size: '340 KB', date: '2026-07-01' },
    { id: 'rep3', name: 'Monthly Maintenance & Diagnostics Ledger', type: 'MONTHLY', period: 'June 2026', format: 'CSV', size: '95 KB', date: '2026-07-01' }
  ]);

  const handleGenerate = (e) => {
    e.preventDefault();
    const newReport = {
      id: `rep${reports.length + 1}`,
      name: `Custom Fleet KPIs (${startDate} to ${endDate})`,
      type: reportType,
      period: `${startDate} to ${endDate}`,
      format: format,
      size: format === 'CSV' ? '45 KB' : format === 'XLSX' ? '280 KB' : '1.4 MB',
      date: new Date().toISOString().split('T')[0]
    };
    setReports(prev => [newReport, ...prev]);
  };

  const chartData = [
    { day: 'Mon', Utilization: 82, Alerts: 2 },
    { day: 'Tue', Utilization: 88, Alerts: 1 },
    { day: 'Wed', Utilization: 91, Alerts: 3 },
    { day: 'Thu', Utilization: 85, Alerts: 0 },
    { day: 'Fri', Utilization: 89, Alerts: 4 },
    { day: 'Sat', Utilization: 76, Alerts: 1 },
    { day: 'Sun', Utilization: 70, Alerts: 2 }
  ];

  const handleShare = (method, reportName) => {
    const link = `https://fleetiq-demo.com/reports/${reportName.replace(/ /g, '_')}`;
    const text = `Check out the latest FleetIQ report: ${reportName} - ${link}`;
    
    if (method === 'whatsapp') {
      window.open(`whatsapp://send?text=${encodeURIComponent(text)}`);
    } else if (method === 'email') {
      window.open(`mailto:?subject=${encodeURIComponent(reportName)}&body=${encodeURIComponent(text)}`);
    } else if (method === 'copy') {
      navigator.clipboard.writeText(link);
      alert('Link copied to clipboard!');
    } else if (method === 'download') {
      alert(`Downloading ${reportName}...`);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
      <div>
        <h1 style={{ fontSize: '32px', margin: '0 0 8px' }}>Reports & Operational KPIs</h1>
        <p style={{ color: 'var(--text-tertiary)' }}>TimescaleDB continuous aggregate audits, custom report generation, and multi-metric leaderboards</p>
      </div>

      {/* KPI Cards Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px' }}>
        {[
          { title: 'Total Distance', value: '15,240 km', desc: '+1,240 km vs last week' },
          { title: 'Avg Fuel Efficiency', value: '4.2 km/L', desc: 'Optimal bounds (4.0 - 4.5)' },
          { title: 'Overall Safety Score', value: '91.2%', desc: 'Safe standing (Deduction -8.8)' },
          { title: 'Fleet Utilization', value: '88.5%', desc: '13/15 Active vehicles' },
          { title: 'Fleet Health Index', value: '92.4%', desc: 'Good standing (1 Active DTC)' },
          { title: 'Avg Trip Duration', value: '180 mins', desc: 'Steady shift margins' },
          { title: 'Avg Idle Time', value: '15.2 mins', desc: 'Goal limit < 20 mins' },
          { title: 'Estimated CO₂ Footprint', value: '9.4 Tonnes', desc: 'Calculated from fuel audit' }
        ].map((kpi, idx) => (
          <Card key={idx} style={{ padding: '20px' }}>
            <span style={{ fontSize: '12px', fontWeight: '600', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>{kpi.title}</span>
            <h2 style={{ fontSize: '24px', margin: '8px 0 4px', fontFamily: 'var(--font-heading)' }}>{kpi.value}</h2>
            <span style={{ fontSize: '11px', color: 'var(--success)' }}>{kpi.desc}</span>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-2">
        {/* Trend Charts */}
        <Card title="Fleet Utilization & Alert Trends (Last 7 Days)">
          <div style={{ width: '100%', height: '250px', marginTop: '16px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                <XAxis dataKey="day" stroke="var(--text-tertiary)" fontSize={11} />
                <YAxis stroke="var(--text-tertiary)" fontSize={11} />
                <Tooltip contentStyle={{ backgroundColor: 'var(--bg-secondary)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }} />
                <Bar dataKey="Utilization" fill="var(--accent)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Custom Report Generator */}
        <Card title="Custom Report Generator">
          <form onSubmit={handleGenerate} style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px' }}>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">Report Interval / Type</label>
                <select className="input-field" value={reportType} onChange={(e) => setReportType(e.target.value)}>
                  <option value="DAILY">Daily Activity Summary</option>
                  <option value="WEEKLY">Weekly Operational KPIs</option>
                  <option value="MONTHLY">Monthly Diagnostics Audit</option>
                  <option value="CUSTOM">Custom Range Aggregate</option>
                </select>
              </div>

              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">Export Format</label>
                <select className="input-field" value={format} onChange={(e) => setFormat(e.target.value)}>
                  <option value="PDF">Acrobat PDF (.pdf)</option>
                  <option value="XLSX">Excel Spreadsheet (.xlsx)</option>
                  <option value="CSV">Comma Separated values (.csv)</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px' }}>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">Start Date</label>
                <input type="date" className="input-field" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
              </div>

              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">End Date</label>
                <input type="date" className="input-field" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
              </div>
            </div>

            <button type="submit" className="btn btn-primary" style={{ padding: '12px', marginTop: '8px' }}>
              <Play size={16} /> Generate & Compile Report
            </button>
          </form>
        </Card>
      </div>

      {/* Downloads ledger */}
      <Card title="Report Audit Ledger">
        <Table
          headers={['Generate Date', 'Report Name', 'Type', 'Period Coverage', 'Format', 'Share & Download']}
          data={reports}
          renderRow={(r) => (
            <tr key={r.id}>
              <td>{r.date}</td>
              <td style={{ fontWeight: '500', maxWidth: '200px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={r.name}>{r.name}</td>
              <td><code>{r.type}</code></td>
              <td>{r.period}</td>
              <td>
                <span className="badge" style={{
                  padding: '2px 6px',
                  fontSize: '11px',
                  backgroundColor: r.format === 'PDF' ? 'var(--danger-bg)' : r.format === 'XLSX' ? 'var(--success-bg)' : 'var(--accent-light)',
                  color: r.format === 'PDF' ? 'var(--danger)' : r.format === 'XLSX' ? 'var(--success)' : 'var(--accent)'
                }}>{r.format}</span>
              </td>
              <td>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <button onClick={() => handleShare('download', r.name)} className="btn btn-secondary" style={{ padding: '4px 8px', fontSize: '11px' }} title="Download">
                    <Download size={14} />
                  </button>
                  <button onClick={() => handleShare('copy', r.name)} className="btn btn-secondary" style={{ padding: '4px 8px', fontSize: '11px' }} title="Copy Link">
                    <Link size={14} />
                  </button>
                  <button onClick={() => handleShare('email', r.name)} className="btn btn-secondary" style={{ padding: '4px 8px', fontSize: '11px' }} title="Share via Email">
                    <Mail size={14} />
                  </button>
                  <button onClick={() => handleShare('whatsapp', r.name)} className="btn" style={{ padding: '4px 8px', fontSize: '11px', backgroundColor: '#25D366', color: 'white' }} title="Share via WhatsApp">
                    <MessageCircle size={14} />
                  </button>
                </div>
              </td>
            </tr>
          )}
        />
      </Card>
    </div>
  );
};
export default ReportsDashboard;
