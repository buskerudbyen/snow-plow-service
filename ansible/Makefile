.PHONY: provision

# ansible

PLAYBOOK_CMD_ASK_PWD:=ANSIBLE_PIPELINING=true ansible-playbook --ask-become --vault-password-file vault-password
PLAYBOOK_CMD:=ANSIBLE_PIPELINING=true ansible-playbook --vault-password-file vault-password

provision: galaxy-install
	${PLAYBOOK_CMD} -i hosts.ini api.yml

galaxy-install:
	ansible-galaxy collection install -r requirements.yml
	ansible-galaxy install -r requirements.yml

galaxy-install-force:
	ansible-galaxy collection install --force -r requirements.yml
	ansible-galaxy install --force -r requirements.yml

