HTML_CONTENT = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Swiggy & Dieto AI Health Coach Simulator</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #FC8019; /* Swiggy Orange */
            --bg-main: #f4f6f8;
            --bg-card: #ffffff;
            --text-main: #1c1c1c;
            --text-muted: #686b78;
            --shadow: 0 8px 30px rgba(0,0,0,0.06);
            --dark-glass: rgba(30, 30, 30, 0.95);
            --border-color: #e9e9eb;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', sans-serif;
        }

        body {
            background-color: var(--bg-main);
            color: var(--text-main);
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }

        header {
            background-color: #ffffff;
            border-bottom: 1px solid var(--border-color);
            padding: 16px 40px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            position: sticky;
            top: 0;
            z-index: 100;
            box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }

        .logo-section {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .logo-swiggy {
            color: var(--primary);
            font-size: 24px;
            font-weight: 700;
            letter-spacing: -0.5px;
        }

        .logo-dieto {
            background: linear-gradient(135deg, #10B981, #059669);
            color: white;
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            text-transform: uppercase;
        }

        .address-badge {
            color: var(--text-muted);
            font-size: 13px;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .search-bar {
            background-color: #f1f1f6;
            border: none;
            padding: 10px 16px;
            border-radius: 8px;
            width: 320px;
            font-size: 14px;
            outline: none;
        }

        .container {
            display: grid;
            grid-template-columns: 1.1fr 0.9fr;
            gap: 32px;
            max-width: 1280px;
            margin: 32px auto;
            width: 100%;
            padding: 0 24px;
            flex-grow: 1;
        }

        .panel-menu {
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .section-title {
            font-size: 20px;
            font-weight: 600;
            margin-bottom: 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .item-card {
            background-color: var(--bg-card);
            border-radius: 12px;
            padding: 24px;
            border: 1px solid var(--border-color);
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: var(--shadow);
            transition: transform 0.2s, box-shadow 0.2s;
        }

        .item-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 12px 40px rgba(0,0,0,0.08);
        }

        .item-details {
            max-width: 70%;
        }

        .item-name {
            font-size: 17px;
            font-weight: 600;
            margin-bottom: 4px;
        }

        .item-meta {
            font-size: 12px;
            color: var(--text-muted);
            margin-bottom: 8px;
            display: flex;
            gap: 12px;
        }

        .item-desc {
            font-size: 13px;
            color: var(--text-muted);
            line-height: 1.4;
        }

        .item-action {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
        }

        .btn-add {
            background-color: white;
            color: #60B246; /* Swiggy Green Add button */
            border: 1px solid var(--border-color);
            padding: 8px 30px;
            font-weight: 600;
            font-size: 13px;
            border-radius: 6px;
            cursor: pointer;
            box-shadow: 0 3px 8px rgba(0,0,0,0.05);
            transition: all 0.15s;
        }

        .btn-add:hover {
            background-color: #f9f9f9;
            box-shadow: 0 4px 12px rgba(0,0,0,0.08);
        }

        .panel-dieto {
            background-color: var(--dark-glass);
            border-radius: 16px;
            padding: 32px;
            color: #ffffff;
            box-shadow: 0 16px 60px rgba(0,0,0,0.15);
            display: flex;
            flex-direction: column;
            gap: 24px;
            height: fit-content;
        }

        .dieto-header {
            border-bottom: 1px solid rgba(255,255,255,0.1);
            padding-bottom: 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .dieto-title {
            font-size: 20px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
            color: #10B981;
        }

        .budget-tracker {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .budget-labels {
            display: flex;
            justify-content: space-between;
            font-size: 13px;
            font-weight: 500;
        }

        .progress-container {
            background-color: rgba(255,255,255,0.1);
            height: 12px;
            border-radius: 6px;
            overflow: hidden;
            position: relative;
        }

        .progress-bar {
            background: linear-gradient(90deg, #10B981, #F59E0B);
            width: 0%;
            height: 100%;
            border-radius: 6px;
            transition: width 0.3s;
        }

        .progress-bar.excessive {
            background: linear-gradient(90deg, #F59E0B, #EF4444);
        }

        .cart-section {
            background-color: rgba(255,255,255,0.03);
            border-radius: 8px;
            padding: 16px;
            border: 1px solid rgba(255,255,255,0.05);
        }

        .cart-title {
            font-size: 14px;
            font-weight: 600;
            margin-bottom: 12px;
            text-transform: uppercase;
            font-size: 11px;
            letter-spacing: 0.5px;
            color: rgba(255,255,255,0.5);
        }

        .cart-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .cart-item {
            display: flex;
            justify-content: space-between;
            font-size: 13px;
        }

        .cart-empty {
            color: rgba(255,255,255,0.3);
            font-size: 13px;
            text-align: center;
            padding: 12px 0;
        }

        .neutralizer-box {
            background-color: rgba(239, 68, 68, 0.1);
            border: 1px solid rgba(239, 68, 68, 0.25);
            border-radius: 8px;
            padding: 16px;
            display: none;
            flex-direction: column;
            gap: 12px;
        }

        .neutralizer-title {
            color: #EF4444;
            font-size: 13px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .neutralizer-desc {
            font-size: 12px;
            color: rgba(255,255,255,0.7);
            line-height: 1.4;
        }

        .neutralizer-options {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .checkbox-container {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 12px;
            cursor: pointer;
            user-select: none;
        }

        .checkbox-container input {
            cursor: pointer;
        }

        .btn-order {
            background-color: var(--primary);
            color: white;
            border: none;
            padding: 16px;
            border-radius: 8px;
            font-weight: 600;
            font-size: 15px;
            cursor: pointer;
            transition: background-color 0.15s;
            text-align: center;
            width: 100%;
        }

        .btn-order:hover {
            background-color: #e06c11;
        }

        .btn-order:disabled {
            background-color: rgba(255,255,255,0.1);
            color: rgba(255,255,255,0.3);
            cursor: not-allowed;
        }

        .terminal-panel {
            background-color: #0d0d0d;
            border-radius: 8px;
            padding: 16px;
            font-family: monospace;
            font-size: 11px;
            color: #00FF66;
            max-height: 120px;
            overflow-y: auto;
            border: 1px solid rgba(255,255,255,0.05);
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .terminal-panel::-webkit-scrollbar {
            width: 4px;
        }

        .terminal-panel::-webkit-scrollbar-thumb {
            background-color: rgba(255,255,255,0.2);
            border-radius: 2px;
        }

        /* Post-Order Section */
        .post-order-panel {
            background-color: var(--bg-card);
            border-radius: 12px;
            padding: 24px;
            border: 1px solid var(--border-color);
            margin-top: 32px;
            box-shadow: var(--shadow);
            display: none;
            flex-direction: column;
            gap: 20px;
        }

        .post-order-title {
            font-size: 16px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .comparison-badge {
            background-color: rgba(16, 185, 129, 0.1);
            color: #10B981;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 500;
            width: fit-content;
        }

        .comparison-badge.warn {
            background-color: rgba(239, 68, 68, 0.1);
            color: #EF4444;
        }

        .suggestion-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
            font-size: 13px;
            color: var(--text-muted);
            line-height: 1.4;
        }

        .suggestion-item {
            display: flex;
            gap: 8px;
        }

        .btn-scan {
            background-color: #10B981;
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 13px;
            cursor: pointer;
            align-self: flex-start;
        }

        .btn-scan:hover {
            background-color: #0d9488;
        }
    </style>
</head>
<body>

<header>
    <div class="logo-section">
        <span class="logo-swiggy">swiggy</span>
        <span class="logo-dieto">Dieto Health Coach</span>
    </div>
    <div class="address-badge">
        📍 <strong>Home</strong> - Ground Floor, Tech Park, Indiranagar
    </div>
    <input type="text" class="search-bar" placeholder="Search for dishes, rolls, biryani...">
</header>

<div class="container">
    <!-- Left Column: Menu Items -->
    <div class="panel-menu">
        <h2 class="section-title">Popular Dinner Choices on Swiggy</h2>
        
        <!-- Menu item 1 -->
        <div class="item-card">
            <div class="item-details">
                <h3 class="item-name">Chicken Tikka Wrap</h3>
                <div class="item-meta">
                    <span>₹180</span>
                    <span>🔥 480 kcal</span>
                    <span>💪 32g protein</span>
                </div>
                <p class="item-desc">Spicy marinated chicken tikka chunks wrapped in soft wheat tortilla with fresh mint chutney.</p>
            </div>
            <div class="item-action">
                <button class="btn-add" onclick="addToCart('Chicken Tikka Wrap', 480, 32)">+ ADD</button>
            </div>
        </div>

        <!-- Menu item 2 -->
        <div class="item-card">
            <div class="item-details">
                <h3 class="item-name">Egg Biryani</h3>
                <div class="item-meta">
                    <span>₹220</span>
                    <span>🔥 590 kcal</span>
                    <span>💪 24g protein</span>
                </div>
                <p class="item-desc">Fragrant basmati rice cooked with whole boiled eggs, saffron, and aromatic biryani spices.</p>
            </div>
            <div class="item-action">
                <button class="btn-add" onclick="addToCart('Egg Biryani', 590, 24)">+ ADD</button>
            </div>
        </div>

        <!-- Menu item 3 -->
        <div class="item-card">
            <div class="item-details">
                <h3 class="item-name">Paneer Rice Bowl</h3>
                <div class="item-meta">
                    <span>₹190</span>
                    <span>🔥 560 kcal</span>
                    <span>💪 20g protein</span>
                </div>
                <p class="item-desc">Paneer cubes tossed in makhani gravy served over a bed of jeera basmati rice.</p>
            </div>
            <div class="item-action">
                <button class="btn-add" onclick="addToCart('Paneer Rice Bowl', 560, 20)">+ ADD</button>
            </div>
        </div>

        <!-- Menu item 4 -->
        <div class="item-card">
            <div class="item-details">
                <h3 class="item-name">Double Cheese Burger</h3>
                <div class="item-meta">
                    <span>₹240</span>
                    <span>🔥 680 kcal</span>
                    <span>💪 28g protein</span>
                </div>
                <p class="item-desc">Juicy double vegetable patty burger with double cheddar cheese and secret burger sauce.</p>
            </div>
            <div class="item-action">
                <button class="btn-add" onclick="addToCart('Double Cheese Burger', 680, 28)">+ ADD</button>
            </div>
        </div>

        <!-- Menu item 5 -->
        <div class="item-card">
            <div class="item-details">
                <h3 class="item-name">Tandoori Chicken Salad</h3>
                <div class="item-meta">
                    <span>₹260</span>
                    <span>🔥 350 kcal</span>
                    <span>💪 35g protein</span>
                </div>
                <p class="item-desc">Grilled tandoori chicken breast strips served on fresh lettuce, cucumber, and cherry tomatoes.</p>
            </div>
            <div class="item-action">
                <button class="btn-add" onclick="addToCart('Tandoori Chicken Salad', 350, 35)">+ ADD</button>
            </div>
        </div>

        <!-- Menu item 6 -->
        <div class="item-card">
            <div class="item-details">
                <h3 class="item-name">Butter Chicken & Naan</h3>
                <div class="item-meta">
                    <span>₹320</span>
                    <span>🔥 850 kcal</span>
                    <span>💪 38g protein</span>
                </div>
                <p class="item-desc">Rich butter chicken curry served with two freshly baked tandoori butter naans.</p>
            </div>
            <div class="item-action">
                <button class="btn-add" onclick="addToCart('Butter Chicken & Naan', 850, 38)">+ ADD</button>
            </div>
        </div>

        <!-- Post-Order Panel (Scanned Comparison Simulator) -->
        <div class="post-order-panel" id="postOrderPanel">
            <div class="post-order-title">🍽️ Post-Order Plate Verification</div>
            <div id="comparisonSummary" class="comparison-badge">Scanning plate...</div>
            <p style="font-size: 13px; color: var(--text-muted);">
                When the delivery arrives, take a photo in the Dieto App. We compare what you ordered to what's actually on the plate to verify nutrition portion variances:
            </p>
            <div id="plateNutritionDetails" style="font-size: 13px; margin: 8px 0; line-height: 1.5;"></div>
            <div class="suggestion-list" id="recoveryCoachList"></div>
            <button class="btn-scan" onclick="triggerSimulatedScan()">📷 Scan & Recalculate Plate Portion</button>
        </div>
    </div>

    <!-- Right Column: Dieto Coach Panel -->
    <div class="panel-dieto">
        <div class="dieto-header">
            <div class="dieto-title">🥗 Dieto Coach Tracker</div>
            <span style="font-size: 12px; color: rgba(255,255,255,0.6);">Mode: Weight Loss</span>
        </div>

        <!-- Budget Tracker -->
        <div class="budget-tracker">
            <div class="budget-labels">
                <span>Daily Calories Intake</span>
                <span id="calorieValue">1,420 / 2,000 kcal</span>
            </div>
            <div class="progress-container">
                <div class="progress-bar" id="progressBar"></div>
            </div>
            <div class="budget-labels" style="font-size: 11px; margin-top: 4px; color: rgba(255,255,255,0.6);">
                <span id="remainingCalories">Remaining: 580 kcal</span>
                <span id="dietoTargetScore">Goal: 2,000 kcal</span>
            </div>
        </div>

        <!-- Cart List -->
        <div class="cart-section">
            <div class="cart-title">Your Swiggy Cart</div>
            <div class="cart-list" id="cartList">
                <div class="cart-empty">Your cart is empty. Add food items from the menu.</div>
            </div>
        </div>

        <!-- Neutralizer Box -->
        <div class="neutralizer-box" id="neutralizerBox">
            <div class="neutralizer-title">⚠️ Calorie Target Exceeded</div>
            <p class="neutralizer-desc">
                Your pending order puts you above your daily budget. Choose a wellness option to balance your day:
            </p>
            <div class="neutralizer-options">
                <label class="checkbox-container">
                    <input type="checkbox" id="chkMintJuice" onchange="recalculateTotals()">
                    🍵 Swap/Add Mint Juice (-150 kcal detox balancer)
                </label>
                <label class="checkbox-container">
                    <input type="checkbox" id="chkExercise" onchange="recalculateTotals()">
                    🏃 Accept easy walking suggestion (10–20 mins post-meal)
                </label>
            </div>
        </div>

        <!-- Order Button -->
        <button class="btn-order" id="btnOrder" disabled onclick="checkoutOrder()">Place Swiggy Order (0 kcal)</button>

        <!-- MCP Terminal Console Logs -->
        <div class="terminal-panel" id="terminalLogs">
            [MCP System] Ready to route Swiggy orders.
        </div>
    </div>
</div>

<script>
    let baseCalories = 1420;
    let baseProtein = 72;
    let targetCalories = 2000;
    
    let cart = [];
    let orderPlacedId = "";

    function addToCart(name, calories, protein) {
        cart.push({name, calories, protein});
        logTerminal(`[MCP] Added to cart: update_food_cart("${name}")`);
        updateCartUI();
    }

    function updateCartUI() {
        const cartList = document.getElementById('cartList');
        if (cart.length === 0) {
            cartList.innerHTML = '<div class="cart-empty">Your cart is empty. Add food items from the menu.</div>';
            document.getElementById('btnOrder').disabled = true;
            document.getElementById('neutralizerBox').style.display = 'none';
            updateProgress(0);
            return;
        }

        cartList.innerHTML = '';
        let totalCartCal = 0;
        cart.forEach((item, idx) => {
            totalCartCal += item.calories;
            const div = document.createElement('div');
            div.className = 'cart-item';
            div.innerHTML = `<span>${item.name}</span><span>${item.calories} kcal <a href="#" style="color:#EF4444; margin-left:8px; text-decoration:none;" onclick="removeFromCart(${idx})">✖</a></span>`;
            cartList.appendChild(div);
        });

        // Check if exceeded
        const totalProjected = baseCalories + totalCartCal;
        const remaining = targetCalories - baseCalories;
        const progressPct = Math.min(100, (totalProjected / targetCalories) * 100);
        updateProgress(progressPct, totalProjected);

        const neutralizerBox = document.getElementById('neutralizerBox');
        if (totalProjected > targetCalories) {
            neutralizerBox.style.display = 'flex';
        } else {
            neutralizerBox.style.display = 'none';
            document.getElementById('chkMintJuice').checked = false;
            document.getElementById('chkExercise').checked = false;
        }

        recalculateTotals();
    }

    function removeFromCart(idx) {
        logTerminal(`[MCP] Removed item from cart index: ${idx}`);
        cart.splice(idx, 1);
        updateCartUI();
    }

    function updateProgress(pct, projected = baseCalories) {
        const bar = document.getElementById('progressBar');
        const calValue = document.getElementById('calorieValue');
        const remainingVal = document.getElementById('remainingCalories');

        bar.style.width = `${pct || ((baseCalories / targetCalories) * 100)}%`;
        
        if (projected > targetCalories) {
            bar.classList.add('excessive');
            remainingVal.innerText = `Excess: ${projected - targetCalories} kcal`;
            remainingVal.style.color = '#EF4444';
        } else {
            bar.classList.remove('excessive');
            remainingVal.innerText = `Remaining: ${targetCalories - projected} kcal`;
            remainingVal.style.color = 'rgba(255,255,255,0.6)';
        }
        
        calValue.innerText = `${projected} / ${targetCalories} kcal`;
    }

    function recalculateTotals() {
        let totalCartCal = cart.reduce((sum, item) => sum + item.calories, 0);
        const totalProjected = baseCalories + totalCartCal;
        const hasExcess = totalProjected > targetCalories;
        
        const chkMint = document.getElementById('chkMintJuice');
        let finalOrderCal = totalCartCal;
        
        if (hasExcess && chkMint.checked) {
            finalOrderCal = Math.max(0, finalOrderCal - 150);
        }

        const btnOrder = document.getElementById('btnOrder');
        btnOrder.disabled = cart.length === 0;
        btnOrder.innerText = `Place Swiggy Order (${finalOrderCal} kcal)`;
    }

    function logTerminal(msg) {
        const console = document.getElementById('terminalLogs');
        const time = new Date().toLocaleTimeString();
        console.innerHTML += `<br>[${time}] ${msg}`;
        console.scrollTop = console.scrollHeight;
    }

    async function checkoutOrder() {
        document.getElementById('btnOrder').disabled = true;
        logTerminal("[MCP] Initializing Swiggy MCP checkout tools...");
        
        // Mock get_addresses
        setTimeout(() => {
            logTerminal("[MCP] Address resolved: addr_01HXYZ");
            
            // Mock placing order
            setTimeout(() => {
                const orderId = "ord_swiggy_7711";
                orderPlacedId = orderId;
                logTerminal(`[MCP] Success! Order placed. ID: ${orderId}`);
                
                // Read checkboxes
                const chkMint = document.getElementById('chkMintJuice').checked;
                const chkExercise = document.getElementById('chkExercise').checked;

                // POST API call to fastapi sync-order
                fetch('/sync-order', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ order_id: orderId })
                })
                .then(r => r.json())
                .then(data => {
                    let finalAdded = data.total_calories;
                    if (chkMint) {
                        finalAdded = Math.max(0, finalAdded - 150);
                    }
                    baseCalories += finalAdded;
                    
                    logTerminal(`[SYSTEM] Synced Dieto Nutrition logs. Added +${finalAdded} kcal.`);
                    updateProgress((baseCalories / targetCalories) * 100);
                    
                    // Show plate scanner
                    document.getElementById('postOrderPanel').style.display = 'flex';
                    document.getElementById('comparisonSummary').innerText = "Awaiting delivery scan... 🍕";
                    
                    // Reset cart
                    cart = [];
                    updateCartUI();
                });
            }, 1000);
        }, 800);
    }

    function triggerSimulatedScan() {
        if (!orderPlacedId) return;
        
        document.getElementById('comparisonSummary').innerText = "📷 Scanning camera feed... Detecting items...";
        
        setTimeout(() => {
            // Call compare-plate API
            fetch('/compare-plate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    order_id: orderPlacedId,
                    detected_items: ["Chicken Biryani", "Chicken 65", "Raita"]
                })
            })
            .then(r => r.json())
            .then(data => {
                const badge = document.getElementById('comparisonSummary');
                badge.innerText = data.comparison_result;
                
                if (data.calorie_difference > 0) {
                    badge.className = "comparison-badge warn";
                } else {
                    badge.className = "comparison-badge";
                }

                // Show Plate details
                const details = document.getElementById('plateNutritionDetails');
                details.innerHTML = `<strong>Swiggy Order Estimate</strong>: ${data.order_estimated_calories} kcal<br>` +
                                    `<strong>Actual Scanned Plate Estimate</strong>: ${data.plate_scanned_calories} kcal<br>` +
                                    `<strong>Divergence</strong>: ${data.calorie_difference > 0 ? '+' : ''}${data.calorie_difference} kcal`;

                // Render Coach Recommendations
                const coachList = document.getElementById('recoveryCoachList');
                coachList.innerHTML = '<strong>Dieto Health Coach Recovery Actions</strong>:';
                data.coach_advice.recommendations.forEach(rec => {
                    const div = document.createElement('div');
                    div.className = 'suggestion-item';
                    div.innerHTML = `<span>${rec}</span>`;
                    coachList.appendChild(div);
                });
            });
        }, 1200);
    }
</script>

</body>
</html>
"""
