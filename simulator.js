const https = require('https');

// Vehicle IDs from the seeded database (Omega Logistics)
const vehicles = [
    {
        id: '44444444-0001-0000-0000-000000000001', // KA-01-MJ-1024
        lat: 12.971598, // Starting in Bangalore
        lng: 77.594562,
        speed: 40,
        heading: 45
    },
    {
        id: '44444444-0001-0000-0000-000000000002', // KA-03-MK-4512
        lat: 12.934533, // Starting in Koramangala
        lng: 77.626579,
        speed: 35,
        heading: 45
    }
];

const API_BASE_URL = 'https://fleetiq-api-gateway.onrender.com/api/v1';
let authToken = null;

async function login() {
    return new Promise((resolve, reject) => {
        const data = JSON.stringify({
            email: 'simulator@fleetiq.internal',
            password: 'simulator123'
        });

        const options = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(data)
            }
        };

        const req = https.request(`${API_BASE_URL}/auth/login`, options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                if (res.statusCode === 200) {
                    const responseData = JSON.parse(body);
                    authToken = responseData.accessToken;
                    console.log('✅ Simulator authenticated successfully!');
                    resolve(authToken);
                } else {
                    reject(`Login failed: ${res.statusCode} - ${body}`);
                }
            });
        });

        req.on('error', reject);
        req.write(data);
        req.end();
    });
}

function startSimulation() {
    console.log('📡 Sending live GPS data to Render API Gateway...');
    
    setInterval(() => {
        vehicles.forEach(v => {
            // Simulate real driving movement in a specific direction (Heading)
            v.lat += 0.0002; // Moving North
            v.lng += 0.0001; // Moving East
            
            // Smoothly vary the speed between 40-80 km/h
            v.speed = Math.max(40, Math.min(80, v.speed + (Math.random() - 0.5) * 5)); 
            
            const url = `${API_BASE_URL}/vehicles/${v.id}/location?lat=${v.lat.toFixed(6)}&lng=${v.lng.toFixed(6)}&speed=${Math.round(v.speed)}&heading=${Math.round(v.heading)}`;
            
            const req = https.request(url, { 
                method: 'PUT', 
                headers: { 
                    'Authorization': `Bearer ${authToken}`
                } 
            }, (res) => {
                if (res.statusCode === 200) {
                    console.log(`[SIMULATOR] Successfully moved Vehicle ${v.id.split('-')[0]} -> Speed: ${Math.round(v.speed)} km/h`);
                } else {
                    console.error(`[SIMULATOR] Error updating location: HTTP ${res.statusCode}`);
                }
            });
            
            req.on('error', (e) => console.error(e));
            req.end();
        });
    }, 3000);
}

async function run() {
    console.log('🚀 Starting FleetIQ Telemetry Simulator...');
    try {
        await login();
        startSimulation();
    } catch (e) {
        console.error('❌ Failed to start simulator:', e);
    }
}

run();
