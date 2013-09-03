
def configTxt = '''
environments {
    admin {
        field_assistant_u = 'medic'
        medical_officer_u = 'tech'
    }
}
'''


def config = new ConfigSlurper("$user").parse(configTxt)
config

