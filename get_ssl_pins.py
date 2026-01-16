
import ssl
import socket
import hashlib
import base64
from urllib.parse import urlparse

def get_certificate_pins(hostname, port=443):
    print(f"Fetching certificates for {hostname}:{port}...")
    
    context = ssl.create_default_context()
    # We want to retrieve the full chain
    
    with socket.create_connection((hostname, port)) as sock:
        with context.wrap_socket(sock, server_hostname=hostname) as ssock:
            # Get the peer certificate
            # Note: Python's ssl module doesn't easily expose the full chain sent by server in a list
            # But the peer cert is the leaf.
            # To get the full chain efficiently usually requires lower level OpenSSL calls or just trusting the Leaf + knowing the Root.
            # However, for pinning, pinning the Leaf and the Intermediate is best.
            
            # This simplified script fetches the LEAF certificate.
            # For a robust "Auto Certificate" tool, we should print the SPKI hash.
            
            cert_bin = ssock.getpeercert(binary_form=True)
            pub_key_info = extract_public_key_info(cert_bin)
            sha256 = hashlib.sha256(pub_key_info).digest()
            pin = base64.b64encode(sha256).decode('utf-8')
            
            print("\n--- Leaf Certificate (Valid for ~90 days) ---")
            print(f"<pin digest=\"SHA-256\">{pin}</pin>")
            
            # Note: Fetching intermediate/root programmatically via Python ssl module defaults is tricky
            # because verify_mode=CERT_REQUIRED validates it but doesn't return the whole chain object easily.
            print("\nTo get the full chain (Intermediate + Root), run this command:")
            print(f"openssl s_client -servername {hostname} -connect {hostname}:{port} -showcerts < /dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64")

def extract_public_key_info(cert_der):
    # This is a hacky way to get SPKI from DER without cryptography lib dependency if possible,
    # but 'ssl' module in python provides the cert.
    # Actually, extracting standard SPKI (SubjectPublicKeyInfo) from X.509 DER is standard.
    # We can use the 'cryptography' library if installed, but better to use standard libs if possible?
    # Standard Python doesn't allow easy SPKI extraction.
    # Let's use a simple openssl command wrapper instead for reliability for the user.
    return b""

if __name__ == "__main__":
    HOST = "zerokeep.vercel.app"
    
    print(f"Generating SSL Pins for {HOST} using OpenSSL...")
    import subprocess
    
    cmd = f"openssl s_client -servername {HOST} -connect {HOST}:443 -showcerts < /dev/null 2>/dev/null"
    # We need to split the output into individual certs
    
    try:
        result = subprocess.check_output(cmd, shell=True).decode('utf-8')
        certs = []
        current_cert = []
        in_cert = False
        
        for line in result.splitlines():
            if "-----BEGIN CERTIFICATE-----" in line:
                in_cert = True
                current_cert = [line]
            elif "-----END CERTIFICATE-----" in line:
                in_cert = False
                current_cert.append(line)
                certs.append("\n".join(current_cert))
            elif in_cert:
                current_cert.append(line)
                
        print(f"Found {len(certs)} certificates in the chain.\n")
        
        labels = ["Leaf (Rotates ~90 days)", "Intermediate (Back up)", "Root (Long Term)"]
        
        for i, cert_pem in enumerate(certs):
            label = labels[i] if i < len(labels) else "Other"
            
            # Pipe to openssl to get SPKI hash
            p1 = subprocess.Popen(['openssl', 'x509', '-pubkey', '-noout'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            pubkey_pem, _ = p1.communicate(input=cert_pem)
            
            p2 = subprocess.Popen(['openssl', 'pkey', '-pubin', '-outform', 'der'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            # Input to pkey must be encoded
            # Actually easier to use p2.communicate(input=pubkey_pem) but need byte/str handling
            
            # Let's just write to tmp file or simpler:
            # Use one-liner per cert
            
            process = subprocess.run(
                f"echo '{cert_pem}' | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64",
                shell=True, capture_output=True, text=True
            )
            pin = process.stdout.strip()
            
            print(f"<!-- {label} -->")
            print(f'<pin digest="SHA-256">{pin}</pin>')
            
    except Exception as e:
        print(f"Error: {e}")
        print("Ensure 'openssl' is installed and available in PATH.")

