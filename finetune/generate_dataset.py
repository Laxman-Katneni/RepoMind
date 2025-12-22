#!/usr/bin/env python3
import json

def generate_dataset():
    examples = []
    
    # Function to create example
    def ex(lang, severity, category, title, message, suggestion, code):
        return {
            "instruction": "Analyze the following code for architectural flaws, security vulnerabilities, and design anti-patterns. Return the result in strict JSON format.",
            "input": code,
            "output": json.dumps({
                "severity": severity,
                "category": category,
                "language": lang,
                "title": title,
                "message": message,
                "suggestion": suggestion
            })
        }
    
    # Java Examples
    java_ex = [
        ex("Java", "CRITICAL", "SECURITY", "SQL Injection", 
           "Direct string concatenation in SQL creates injection vulnerability",
           "Use PreparedStatement with parameterized queries",
           'String query = "SELECT * FROM users WHERE id = " + userId;\nStatement stmt = conn.createStatement();\nResultSet rs = stmt.executeQuery(query);'),
        
        ex("Java", "WARNING", "ARCHITECTURE", "God Class",
           "Single class handling multiple responsibilities violates SRP",
           "Split into OrderValidator, PaymentProcessor, EmailService",
           '@Service\npublic class OrderService {\n    void validate() {}\n    void charge() {}\n    void email() {}\n    void log() {}\n}'),
        
        ex("Java", "CRITICAL", "SECURITY", "Hardcoded Secret",
           "JWT secret in source code enables token forgery",
           "Load from environment: System.getenv(\"JWT_SECRET\")",
           'private static final String JWT_SECRET = "my-secret-key-123";'),
        
        ex("Java", "WARNING", "ARCHITECTURE", "Entity Exposure",
           "JPA entities exposed to API layer leak database schema",
           "Create DTOs for API contracts, map in service layer",
           '@GetMapping("/users/{id}")\npublic User getUser(@PathVariable Long id) {\n    return userRepo.findById(id).get();\n}'),
        
        ex("Java", "CRITICAL", "SECURITY", "Path Traversal",
           "Unsanitized filename allows directory traversal attacks",
           "Use Paths.get(filename).getFileName() or UUID.randomUUID()",
           'String path = "/uploads/" + request.getParameter("filename");\nFile file = new File(path);'),
        
        ex("Java", "WARNING", "BEST_PRACTICE", "Missing Transaction",
           "Multi-step database operations without @Transactional risk partial updates",
           "Add @Transactional to ensure atomicity",
           'public void transfer(Account from, Account to, BigDecimal amount) {\n    from.debit(amount);\n    to.credit(amount);\n}'),
        
        ex("Java", "WARNING", "ARCHITECTURE", "Tight Coupling",
           "Direct instantiation creates hard dependencies",
           "Use dependency injection via constructor",
           'public class UserService {\n    private EmailService emailService = new EmailService();\n}'),
        
        ex("Java", "CRITICAL", "SECURITY", "XXE Vulnerability",
           "XML parser without disabling external entities",
           "factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)",
           'DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();\nDocument doc = factory.newDocumentBuilder().parse(inputStream);'),
        
        ex("Java", "WARNING", "BEST_PRACTICE", "Resource Leak",
           "InputStream not closed on exception",
           "Use try-with-resources",
           'InputStream is = new FileInputStream(file);\nString content = new String(is.readAllBytes());\nis.close();'),
        
        ex("Java", "WARNING", "ARCHITECTURE", "Synchronous in Async",
           "Blocking call in async method defeats reactive programming",
           "Use WebClient or reactive database drivers",
           '@Async\npublic CompletableFuture<User> getUser(Long id) {\n    return CompletableFuture.completedFuture(restTemplate.getForObject(url, User.class));\n}')
    ]
    
    # Python Examples
    python_ex = [
        ex("Python", "CRITICAL", "SECURITY", "Pickle Vulnerability",
           "Deserializing untrusted pickle enables arbitrary code execution",
           "Use JSON/joblib or restrict with __reduce_ex__ override",
           'import pickle\nwith open(file_path, "rb") as f:\n    data = pickle.load(f)'),
        
        ex("Python", "CRITICAL", "ARCHITECTURE", "Blocking Event Loop",
           "Synchronous I/O in async corrupts event loop performance",
           "Use aiohttp: async with session.get(url) as resp",
           'async def fetch(url):\n    response = requests.get(url)\n    return response.json()'),
        
        ex("Python", "WARNING", "BEST_PRACTICE", "Mutable Default",
           "Mutable default arguments share state across calls",
           "Use None: def func(items=None): items = items or []",
           'def add_item(item, items=[]):\n    items.append(item)\n    return items'),
        
        ex("Python", "CRITICAL", "SECURITY", "Command Injection",
           "User input in shell command enables arbitrary execution",
           "Use list args: subprocess.run([\"cmd\", arg])",
           'import  subprocess\nsubprocess.call(f"echo {user_input}", shell=True)'),
        
        ex("Python", "CRITICAL", "SECURITY", "Hardcoded Credentials",
           "API keys in source code leak in VCS",
           "Use os.getenv(\"API_KEY\") or secret managers",
           'API_KEY = "sk-1234567890abcdef"\nheaders = {"Authorization": f"Bearer {API_KEY}"}'),
        
        ex("Python", "WARNING", "ARCHITECTURE", "Circular Import",
           "Mutual imports create coupling and runtime errors",
           "Refactor to shared module or use lazy imports",
           '# module_a.py\nfrom module_b import ClassB\n\nclass ClassA:\n    def use(self): ClassB()'),
        
        ex("Python", "WARNING", "BEST_PRACTICE", "Bare Except",
           "Catching all exceptions hides errors and hampers debugging",
           "Catch specific exceptions: except ValueError",
           'try:\n    risky_operation()\nexcept:\n    pass'),
        
        ex("Python", "WARNING", "ARCHITECTURE", "Missing Type Hints",
           "Dynamic typing without hints reduces code clarity",
           "Add type hints: def func(x: int) -> str",
           'def process(data):\n    result = transform(data)\n    return result'),
        
        ex("Python", "CRITICAL", "SECURITY", "SQL Injection",
           "String formatting in SQL queries enables injection",
           "Use parameterized queries: cursor.execute(sql, (val,))",
           'cursor.execute(f"SELECT * FROM users WHERE name = \'{name}\'")'),
        
        ex("Python", "WARNING", "BEST_PRACTICE", "Global State",
           "Global variables create hidden dependencies",
           "Use dependency injection or class attributes",
           'counter = 0\ndef increment():\n    global counter\n    counter += 1')
    ]
    
    # TypeScript Examples
    ts_ex = [
        ex("TypeScript", "WARNING", "BEST_PRACTICE", "Missing Dependencies",
           "useEffect without proper deps causes stale closures",
           "Add all dependencies: useEffect(() => {...}, [userId])",
           'useEffect(() => {\n    fetchUser(userId);\n}, []);'),
        
        ex("TypeScript", "CRITICAL", "SECURITY", "XSS Vulnerability",
           "Unsanitized HTML rendering enables script injection",
           "Use DOMPurify.sanitize() before rendering",
           '<div dangerouslySetInnerHTML={{ __html: userContent }} />'),
        
        ex("TypeScript", "WARNING", "ARCHITECTURE", "Callback Hell",
           "Nested callbacks create unmaintainable pyramid of doom",
           "Use async/await: const r = await getData(); await process(r)",
           'getData(result => {\n    process(result, processed => {\n        save(processed, response => callback(response));\n    });\n});'),
        
        ex("TypeScript", "CRITICAL", "SECURITY", "NoSQL Injection",
           "Unsanitized MongoDB queries enable data extraction",
           "Validate input types and use schema validation",
           'const user = await User.findOne({ username: req.body.username });'),
        
        ex("TypeScript", "WARNING", "SECURITY", "Prototype Pollution",
           "Object.assign with untrusted input pollutes prototypes",
           "Validate keys or use Object.create(null)",
           'const config = {};\nObject.assign(config, JSON.parse(userInput));'),
        
        ex("TypeScript", "WARNING", "BEST_PRACTICE", "Any Type Abuse",
           "Type assertions to any eliminate type safety",
           "Define proper interfaces for type safety",
           'function process(data: any): any {\n    return data.map((x: any) => x.value);\n}'),
        
        ex("TypeScript", "WARNING", "ARCHITECTURE", "Prop Drilling",
           "Passing props through multiple layers creates coupling",
           "Use React Context or state management library",
           '<Component user={user} theme={theme} settings={settings} />'),
        
        ex("TypeScript", "WARNING", "BEST_PRACTICE", "Ignoring Promises",
           "Async functions called without await or .catch()",
           "Add await or .catch() for error handling",
           'async function save() {}\nsave(); // Fire and forget'),
        
        ex("TypeScript", "CRITICAL", "SECURITY", "Eval Injection",
           "Using eval() with user input enables code execution",
           "Use JSON.parse() or safe expression evaluators",
           'const result = eval(userInput);'),
        
        ex("TypeScript", "WARNING", "ARCHITECTURE", "State Mutation",
           "Directly mutating state breaks React rendering",
           "Use immutable updates: setState([...items, newItem])",
           'const addItem = () => {\n    items.push(newItem);\n    setItems(items);\n};')
    ]
    
    # Combine and multiply to reach 200
    base = java_ex + python_ex + ts_ex
    while len(examples) < 200:
        examples.extend(base)
    
    return examples[:200]

if __name__ == "__main__":
    dataset = generate_dataset()
    with open("synthetic_train.jsonl", "w") as f:
        for example in dataset:
            f.write(json.dumps(example) + "\n")
    print(f"✅ Generated {len(dataset)} examples in synthetic_train.jsonl")
