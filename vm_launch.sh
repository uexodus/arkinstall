set -e
source vm.env

if [[ $EUID -eq 0 ]]; then
  echo "This script should not be run as root."
  echo "Running as root would cause build files to be owned by root."
  exit 1
fi

sudo pacman -Sy archiso qemu edk2-ovmf --noconfirm
sudo mkdir -p "$TEST_DIR" "$WORK_DIR" "$SHARE_DIR"
sudo mkdir -p "$WORK_DIR/airootfs/root"

cat <<- _EOF_ | sudo tee "$WORK_DIR"/airootfs/root/.zprofile
  mkdir -p /media/share
  mount -t 9p -o trans=virtio host_share /media/share
  cd /media/share
_EOF_

sudo cp -r /usr/share/archiso/configs/releng/* "$WORK_DIR"

sudo sed -i 's/^timeout=15$/timeout=1/' "$WORK_DIR/grub/grub.cfg"
sudo tee -a "$WORK_DIR/airootfs/etc/ssh/sshd_config" > /dev/null <<EOF
PermitRootLogin yes
PermitEmptyPasswords yes
EOF

bash ./vm_update_binary.sh

cd "$WORK_DIR"
sudo mkarchiso -v -w work/ -o out/ .

if [ ! -f "./test.qcow2" ]; then
	sudo qemu-img create -f qcow2 ./test.qcow2 15G
fi

if sudo lsof ./test.qcow2 &>/dev/null; then
    echo "test.qcow2 is locked by another process. Attempting to kill it..."
    sudo fuser -k ./test.qcow2 || true
fi

sudo qemu-system-x86_64 \
        -cpu host \
        -enable-kvm \
        -machine q35,accel=kvm \
        -device intel-iommu \
        -m 2048 \
        -nic user,hostfwd=tcp:127.0.0.1:2222-:22 \
        -drive if=pflash,format=raw,readonly=on,file=/usr/share/edk2-ovmf/x64/OVMF_CODE.4m.fd  \
        -drive if=pflash,format=raw,readonly=on,file=/usr/share/edk2-ovmf/x64/OVMF_VARS.4m.fd \
        -device virtio-scsi-pci,bus=pcie.0,id=scsi0 \
        -device scsi-hd,drive=hdd0,bus=scsi0.0,id=scsi0.0,bootindex=2 \
        -drive file=./test.qcow2,if=none,format=qcow2,discard=unmap,aio=native,cache=none,id=hdd0 \
        -device virtio-scsi-pci,bus=pcie.0,id=scsi1 \
        -device scsi-cd,drive=cdrom0,bus=scsi1.0,bootindex=1 \
        -drive file="$(find "$WORK_DIR"/out/ -name "*.iso" | head -n 1)",media=cdrom,if=none,format=raw,cache=none,id=cdrom0 \
        -fsdev local,id=fsdev0,path="$SHARE_DIR",security_model=none \
        -device virtio-9p-pci,id=fs0,fsdev=fsdev0,mount_tag=host_share \
        &
QEMU_PID=$!

if ! ps -p "$QEMU_PID" > /dev/null; then
    echo "QEMU failed to start or exited early."
    exit 1
fi

trap 'echo "Cleaning up..."; kill $QEMU_PID 2>/dev/null' EXIT

ssh_vm() {
  ssh -q \
      -o ConnectTimeout=3 \
      -o StrictHostKeyChecking=no \
      -o UserKnownHostsFile=/dev/null \
      -p 2222 root@localhost "$@"
}

echo "Launched QEMU VM. Waiting for SSH to become available..."
for i in {1..60}; do
    if ssh_vm true; then
        echo "SSH is ready"
        break
    fi
    sleep 1
    echo "Attempt $i/60"
done

ssh_vm