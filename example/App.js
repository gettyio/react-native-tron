import React, { Component } from 'react';
import {
  Platform,
  StyleSheet,
  Text,
  View,
  TextInput,
  ScrollView
} from 'react-native';
import RNTron from 'react-native-tron';

export default class App extends Component {

  constructor() {
    super();

    this.state = {
      address: null,
      privateKey: null,
      mnemonics: null
    }
  }

  async componentDidMount() {
    var mnemonics = "hub purpose pistol mountain tape possible aware board decorate good chair only";
    var generatedKeypair = await RNTron.generateKeypair(mnemonics, 0, false);

    this.setState({
      address: generatedKeypair.address,
      privateKey: generatedKeypair.privateKey,
      publicKey: generatedKeypair.publicKey,
      password: generatedKeypair.password,
      mnemonics: mnemonics
    });
  }

  render() {
    return (
      <ScrollView>
      <View style={styles.container}>
        <Text style={styles.title}>
          Generated Tron Account
        </Text>
        <Text style={styles.subtitle}>From Mnemonics</Text>
        <TextInput style={styles.value} multiline={true}>{ this.state.mnemonics }</TextInput>
        <Text style={styles.subtitle}>Address</Text>
        <TextInput style={styles.value} multiline={true}>{ this.state.address }</TextInput>
        <Text style={styles.subtitle}>Private Key</Text>
        <TextInput style={styles.value} multiline={true}>{ this.state.privateKey }</TextInput>
        <Text style={styles.subtitle}>Public Key</Text>
        <TextInput style={styles.value} multiline={true}>{ this.state.publicKey }</TextInput>
        <Text style={styles.subtitle}>Password</Text>
        <TextInput style={styles.value} multiline={true}>{ this.state.password }</TextInput>
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
  }
});
