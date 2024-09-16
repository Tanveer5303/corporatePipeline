
### Links to download Prometheus, Node_Exporter & black Box exporter https://prometheus.io/download/
### Links to download Grafana https://grafana.com/grafana/download
### Other link from video https://github.com/prometheus/blackbox_exporter

Monitoring VM Setup
EC2 Instance Configuration
OS: Ubuntu 20.04
Instance Type: t2.medium
Storage: 20 GB
Key: [Your EC2 key]
Security Group: launch-wizard-2
1. Prometheus Setup
Download and Install Prometheus
bash
Copy code
wget https://github.com/prometheus/prometheus/releases/download/v2.54.1/prometheus-2.54.1.linux-amd64.tar.gz
tar -xvf prometheus-2.54.1.linux-amd64.tar.gz
cd prometheus-2.54.1.linux-amd64
Run Prometheus
bash
Copy code
./prometheus &
Prometheus runs by default on port 9090.
2. Grafana Setup
Install Grafana
bash
Copy code
sudo apt-get install -y adduser libfontconfig1 musl
wget https://dl.grafana.com/enterprise/release/grafana-enterprise_11.2.0_amd64.deb
sudo dpkg -i grafana-enterprise_11.2.0_amd64.deb
Start Grafana
bash
Copy code
sudo /bin/systemctl start grafana-server
Grafana will be running on port 3000.
3. Blackbox Exporter Setup
Download and Install Blackbox Exporter
bash
Copy code
wget https://github.com/prometheus/blackbox_exporter/releases/download/v0.25.0/blackbox_exporter-0.25.0.linux-amd64.tar.gz
tar -xvf blackbox_exporter-0.25.0.linux-amd64.tar.gz
cd blackbox_exporter-0.25.0.linux-amd64
./blackbox_exporter &
Blackbox Exporter runs on port 9115.
4. Configure Prometheus for Blackbox Monitoring
Edit prometheus.yml
yaml
Copy code
scrape_configs:
  - job_name: 'blackbox'
    metrics_path: /probe
    params:
      module: [http_2xx]  # Look for a HTTP 200 response.
    static_configs:
      - targets:
          - http://prometheus.io
          - http://<k8s-cluster-worker-node-ip>:32500  # Application URL
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: <ip-of-monitoringVM-blckbox>:9115  # Blackbox exporter's IP and port.
Restart Prometheus
bash
Copy code
pgrep prometheus  # Find the Prometheus process ID.
kill <process_id>  # Kill the process.
./prometheus &
Verify Targets in Prometheus
Go to Prometheus UI -> Status -> Targets.
5. Add Prometheus as Grafana Data Source
Go to Grafana UI -> Connections -> Data Source -> Prometheus.
Provide the IP of Prometheus and click Save & Test.
6. Import Blackbox Dashboard in Grafana
Search for Blackbox Grafana Dashboard on Google or go to: Grafana Blackbox Dashboard
Copy the dashboard ID from the page.
In Grafana, click on + (Create) -> Import Dashboard -> Paste the ID -> Load.
Select Prometheus as the data source and click Import.
7. System-Level Monitoring
Install Node Exporter
bash
Copy code
wget https://github.com/prometheus/node_exporter/releases/download/v1.5.0/node_exporter-1.5.0.linux-amd64.tar.gz
tar -xvf node_exporter-1.5.0.linux-amd64.tar.gz
cd node_exporter-1.5.0.linux-amd64
./node_exporter &
Node Exporter runs on port 9100.
Edit prometheus.yml for Node Exporter and Jenkins
yaml
Copy code
scrape_configs:
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['<monitoring-machine-ip>:9100']  # Node Exporter target

  - job_name: 'jenkins'
    metrics_path: '/prometheus'
    static_configs:
      - targets: ['<jenkins-machine-ip>:8080']  # Jenkins target
8. Create Grafana Dashboard for Node Exporter
Search for Node Exporter Grafana Dashboard on Google.
Copy the dashboard ID.
In Grafana, go to + (Create) -> Import Dashboard -> Paste the ID -> Load.
Select Prometheus as the data source and click Import.
9. Node Exporter for Application to monitor the node
Edit prometheus.yml for Application Node Exporter
yaml
Copy code
- job_name: 'node-exporter-website'
  static_configs:
      - targets: ['<k8s-cluster-worker-noe-ip>:32500']  # Application target
Now you have successfully set up monitoring for both your website and system metrics using Prometheus and Grafana!

