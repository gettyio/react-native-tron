import React, { Component } from 'react';
import {
  Platform,
  StyleSheet,
  Text,
  View,
  TextInput,
  ScrollView,
  TouchableOpacity
} from 'react-native';
import RNTron from 'react-native-tron';

const Button = ({onPress, children}) => (
  <TouchableOpacity onPress={onPress} style={styles.button}>
    <Text style={styles.buttonText} >{children}</Text>
  </TouchableOpacity>
)

export default class App extends Component {

  state = {
    acconts: [],
    value: ''
  }

  async componentDidMount() {
    // var mnemonics = "hub purpose pistol mountain tape possible aware board decorate good chair only";
    // var generatedKeypair = await RNTron.generateKeypair(mnemonics, 0, false);

    // this.setState({
    //   address: generatedKeypair.address,
    //   privateKey: generatedKeypair.privateKey,
    //   publicKey: generatedKeypair.publicKey,
    //   password: generatedKeypair.password,
    //   mnemonics: mnemonics
    // });
  }

  _create = async () => {
    try {
      if(!this.state.value) {
        throw new Error("Pin is required.")
      }
      const acc = await RNTron.createAccount(this.state.value)
      this.setState({
        acconts: [acc, ...this.state.acconts]
      })
    } catch (error) {
      console.warn(error.message)
    }
  }

  render() {
    return (
      <ScrollView>
        <View style={styles.container}>
          <View style={styles.row}>
            <Button onPress={this._create}>Generated Tron Account</Button>
            <TextInput style={styles.input} placeholder="PIN" value={this.state.value} onChangeText={value => this.setState({ value })} />
          </View>
          <View style={styles.body}>
            {this.state.acconts.map(acc=> (
              <View key={acc.publicKey} style={styles.wrapper}>
                <Text style={styles.value}>{acc.address}</Text>
              </View>
            ))}
          </View>
        </View> 
      </ScrollView>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    margin: 10
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
    marginTop: 20,
    marginBottom: 10
  },
  subtitle: {
    fontSize: 16,
    fontWeight: 'bold',
    textAlign: 'center',
    marginTop: 10,
  },
  value: {
    fontSize: 12,
    textAlign: 'center',
  },
  wrapper: {
    padding: 10,
    borderBottomColor: '#000',
    borderBottomWidth: 1
  },
  button: {
    borderColor: "blue",
    borderWidth: 1,
    backgroundColor: "blue",
    padding: 5,
    flex: 4
  },
  buttonText: {
    color: "#fff",
    textAlign: "center"
  },
  body: {
    flex: 9
  }, 
  row: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  input: {
    flex: 6
  }
});
