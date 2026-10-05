const http = require('http');

async function run() {
  console.log("Starting 10,000 requests simulation...");
  
  let successCount = 0;
  let failCount = 0;
  
  const makeRequest = (i) => {
    return new Promise((resolve) => {
      const data = JSON.stringify({
        productId: "prod-100",
        customerId: `sim-cust-${i}`,
        quantity: 1,
        idempotencyKey: `sim-key-${i}`
      });

      const options = {
        hostname: 'localhost',
        port: 8080,
        path: '/api/reservations',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': data.length
        }
      };

      const req = http.request(options, (res) => {
        if (res.statusCode === 200 || res.statusCode === 201) {
          successCount++;
        } else {
          failCount++;
        }
        res.on('data', () => {});
        res.on('end', resolve);
      });

      req.on('error', () => {
        failCount++;
        resolve();
      });

      req.write(data);
      req.end();
    });
  };

  const startTime = Date.now();
  
  // Send in batches of 200 to not overwhelm Node.js socket pool entirely
  for(let b=0; b<50; b++) {
    const batch = [];
    for(let i=0; i<200; i++) {
      batch.push(makeRequest(b*200 + i));
    }
    await Promise.all(batch);
  }

  const duration = Date.now() - startTime;
  console.log(`Finished in ${duration}ms`);
  console.log(`Success: ${successCount}`);
  console.log(`Failed: ${failCount}`);
  
  // Check inventory
  http.get('http://localhost:8080/api/inventory/prod-100', (res) => {
    let raw = '';
    res.on('data', chunk => raw += chunk);
    res.on('end', () => {
      console.log('Final Inventory:', raw);
    });
  });
}

run();
